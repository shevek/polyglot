/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.anarres.graphviz.builder.GraphVizGraph;
import org.anarres.graphviz.builder.GraphVizable;
import org.anarres.polyglot.analysis.AnnotationChecker;
import org.anarres.polyglot.analysis.ConflictChecker;
import org.anarres.polyglot.analysis.EpsilonChecker;
import org.anarres.polyglot.analysis.GrammarNormalizer;
import org.anarres.polyglot.analysis.GrammarWriterVisitor;
import org.anarres.polyglot.analysis.Inliner;
import org.anarres.polyglot.analysis.ModelBuilderVisitor;
import org.anarres.polyglot.analysis.NFABuilderVisitor;
import org.anarres.polyglot.analysis.ReferenceLinker;
import org.anarres.polyglot.analysis.StartChecker;
import org.anarres.polyglot.analysis.TypeChecker;
import org.anarres.polyglot.dfa.DFA;
import org.anarres.polyglot.lexer.Lexer;
import org.anarres.polyglot.lexer.LexerException;
import org.anarres.polyglot.lr.FirstFunction;
import org.anarres.polyglot.lr.FollowFunction;
import org.anarres.polyglot.lr.LR0ItemUniverse;
import org.anarres.polyglot.lr.LR1ItemUniverse;
import org.anarres.polyglot.lr.LRAutomaton;
import org.anarres.polyglot.lr.LRConflict;
import org.anarres.polyglot.diagnoser.AlgorithmicLRDiagnoser;
import org.anarres.polyglot.diagnoser.LRDiagnoser;
import org.anarres.polyglot.lr.LRDiagnosis;
import org.anarres.polyglot.lr.LRState;
import org.anarres.polyglot.lr.TokenSet;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.StateModel;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.node.Start;
import org.anarres.polyglot.output.EncodedStateMachine;
import org.anarres.polyglot.output.OutputData;
import org.anarres.polyglot.output.OutputLanguage;
import org.anarres.polyglot.output.OutputWriter;
import org.anarres.polyglot.parser.Parser;
import org.anarres.polyglot.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.anarres.polyglot.DebugHandler.Target.*;

/**
 *
 * @author shevek
 */
public class PolyglotEngine {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotEngine.class);

    /**
     * Deletes just the children of a directory.
     *
     * Deleting the directory itself confuses the hell out of NetBeans,
     * as it can fail to see the recreation of a source root, whereas
     * it will tend to see the recreation of files within a source root.
     */
    public static void deleteChildren(@Nonnull File dir, @Nonnull String name) throws IOException {
        for (File file : Files.fileTreeTraverser().postOrderTraversal(dir)) {
            // Don't delete the actual output directory as this confuses file-watching IDEs.
            if (dir.equals(file) && dir.isDirectory())
                continue;
            try {
                java.nio.file.Files.delete(file.toPath());
            } catch (NoSuchFileException e) {
            }
        }
    }

    // @ThreadSafe
    /**
     * Thread-safe, throws an exception on failure to create.
     *
     * @see java.nio.file.Files#createDirectories(java.nio.file.Path, java.nio.file.attribute.FileAttribute...)
     */
    public static void mkdirs(@Nonnull File dir, @Nonnull String name) throws IOException {
        java.nio.file.Files.createDirectories(dir.toPath());
    }

    private final ErrorHandler errors = new ErrorHandler();
    private final String name;
    private final CharSource input;
    private final Map<OutputLanguage, File> outputDirs = new EnumMap<>(OutputLanguage.class);
    @Nonnull
    private DebugHandler debugHandler = DebugHandler.None.INSTANCE;
    public final Set<Option> options = EnumSet.of(Option.SLR, Option.LR1, Option.INLINE_TABLES, Option.CG_PARENT, Option.CG_APIDOC, Option.CG_FINDBUGS, Option.PARALLEL);
    private final Table<OutputLanguage, String, File> templates = HashBasedTable.create();

    public PolyglotEngine(@Nonnull String name, @Nonnull CharSource input, @Nonnull File outputDir) {
        this.name = name;
        this.input = input;
        setOutputDir(OutputLanguage.java, outputDir);
    }

    public PolyglotEngine(@Nonnull File input, @Nonnull File outputDir) {
        this(input.getName(), Files.asCharSource(input, StandardCharsets.UTF_8), outputDir);
    }

    @Nonnull
    public CharSource getInput() {
        return input;
    }

    /**
     * Returns the ErrorHandler which contains the list of {@link ErrorHandler.Error Errors}.
     *
     * @return the ErrorHandler which contains the list of {@link ErrorHandler.Error Errors}.
     */
    @Nonnull
    public ErrorHandler getErrors() {
        return errors;
    }

    /**
     * Returns the descriptive name of this PolyglotEngine, for diagnostic purposes.
     *
     * @return the descriptive name of this PolyglotEngine, for diagnostic purposes.
     */
    @Nonnull
    public String getName() {
        return name;
    }

    public void setDebugHandler(@Nonnull DebugHandler debugHandler) {
        this.debugHandler = Preconditions.checkNotNull(debugHandler, "DebugHandler was null.");
    }

    /**
     * Returns the (mutable) set of Options used to configure this PolyglotEngine.
     *
     * @return the (mutable) set of Options used to configure this PolyglotEngine.
     */
    @Nonnull
    public Set<Option> getOptions() {
        return options;
    }

    public void setOption(@Nonnull Option option, boolean value) {
        if (value)
            options.add(option);
        else
            options.remove(option);
    }

    public boolean isOption(@Nonnull Option option) {
        return options.contains(option);
    }

    public void addTemplates(@Nonnull OutputLanguage language, Map<String, File> templates) {
        this.templates.row(language).putAll(templates);
    }

    public void addTemplate(@Nonnull OutputLanguage language, @Nonnull String dstPath, @Nonnull File srcFile) {
        this.templates.put(language, dstPath, srcFile);
    }

    public void setOutputDir(@Nonnull OutputLanguage language, @CheckForNull File outputDir) {
        if (outputDir != null)
            outputDirs.put(language, outputDir);
        else
            outputDirs.remove(language);
    }

    private void dump(@CheckForNull CharSink sink, @Nonnull GraphVizable object) throws IOException {
        if (sink != null) {
            GraphVizGraph graph = new GraphVizGraph();
            object.toGraphViz(graph);
            try (Writer out = sink.openBufferedStream()) {
                graph.writeTo(out);
            }
        }
    }

    private void dump(@CheckForNull CharSink sink, @Nonnull Start ast) throws IOException {
        if (sink != null) {
            GrammarWriterVisitor writer = new GrammarWriterVisitor();
            ast.apply(writer);
            sink.write(writer.toString());
        }
    }

    private void dump(@CheckForNull CharSink sink, @Nonnull GrammarModel grammar) throws IOException {
        // LOG.info(context + "\n" + grammar);
        dump(sink, grammar.toTree());
    }

    private void dump(@CheckForNull CharSink textSink, @CheckForNull CharSink dotSink, @Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot, @Nonnull LRAutomaton automaton) throws IOException {
        dump(dotSink, automaton);

        if (textSink != null) {
            try (Writer writer = textSink.openBufferedStream()) {
                for (LRState state : automaton.getStates()) {
                    writer.write(state.toString());
                    writer.write('\n');
                }

                LRConflict.Map conflicts = automaton.getConflicts();
                writer.write("Conflicts are:\n" + conflicts + "\n");

                if (false) {
                    // For some as-yet-undetermined reason, this makes the system go to lunch.
                    LRDiagnoser diagnoser = newLRDiagnoser(grammar, cstProductionRoot);
                    for (LRConflict conflict : conflicts.values()) {
                        LOG.info("Diagnosing:\n" + conflict);
                        LRDiagnosis diagnosis = diagnoser.diagnose(conflict);
                        LOG.info("Diagnosis:\n" + diagnosis);
                        writer.write(diagnosis.toString());
                    }
                }
            }
        }
    }

    @Nonnull
    protected PolyglotExecutor newExecutor() {
        if (!isOption(Option.PARALLEL))
            return new PolyglotExecutor.Serial();
        int nthreads = Runtime.getRuntime().availableProcessors();
        if (nthreads == 1)
            return new PolyglotExecutor.Serial();
        return new PolyglotExecutor.Parallel(name, nthreads);
    }

    @CheckForNull
    protected Start buildParseTree() throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (BufferedReader in = input.openBufferedStream()) {
            PushbackReader reader = new PushbackReader(in);
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            Start ast = parser.parse();
            // dump("Parsed grammar", ast);
            return ast;
        } catch (LexerException e) {
            errors.addError(e.getToken(), "Failed to lex source file: " + e);
            return null;
        } catch (ParserException e) {
            errors.addError(e.getToken(), "Failed to parse source file: " + e);
            return null;
        } finally {
            LOG.info("{}: Parsing took {}", getName(), stopwatch);
        }
    }

    @Nonnull
    protected GrammarModel buildModel(@Nonnull PolyglotExecutor executor, @Nonnull Start ast) throws IOException, InterruptedException, ExecutionException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        GrammarModel grammar = new GrammarModel();
        ast.apply(new ModelBuilderVisitor(errors, grammar));
        if (errors.isFatal())
            return grammar;
        dump(debugHandler.forTarget(GRAMMAR_PARSED, ".parsed.grammar"), grammar);
        // dump("Raw model", grammar);
        new AnnotationChecker(errors, grammar).run();
        if (errors.isFatal())
            return grammar;
        new ReferenceLinker(errors, grammar).run();
        if (errors.isFatal())
            return grammar;
        dump(debugHandler.forTarget(GRAMMAR_LINKED, ".linked.grammar"), grammar);
        // dump("Linked model", grammar);
        new EpsilonChecker(errors, grammar).run();
        if (errors.isFatal())
            return grammar;
        new StartChecker(errors, grammar).run();
        if (errors.isFatal())
            return grammar;
        new ConflictChecker(errors, grammar).run();
        if (errors.isFatal())
            return grammar;
        dump(debugHandler.forTarget(GRAMMAR_CST, ".cst.dot"), grammar.getCstGraphVizable());
        dump(debugHandler.forTarget(GRAMMAR_AST, ".ast.dot"), grammar.getAstGraphVizable());
        stopwatch.stop();
        buildOutputs(executor, grammar,
                null, Collections.<EncodedStateMachine.Parser>emptyList(),
                Predicates.equalTo(OutputLanguage.html));
        stopwatch.start();

        new GrammarNormalizer(errors, grammar).run();
        dump(debugHandler.forTarget(GRAMMAR_NORMALIZED, ".normalized.grammar"), grammar);
        if (errors.isFatal())
            return grammar;

        new TypeChecker(errors, grammar).run();
        if (errors.isFatal())
            return grammar;
        ast.apply(new NFABuilderVisitor(errors, grammar));

        LOG.info("{}: Building model took {}", getName(), stopwatch);
        return grammar;
    }

    @CheckForNull
    protected EncodedStateMachine.Lexer buildLexer(@Nonnull GrammarModel grammar) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            DFA.TokenMask mask = new DFA.TokenMask(grammar);

            for (StateModel state : grammar.states.values()) {
                if (state.nfa == null)
                    continue;
                dump(debugHandler.forTarget(STATE_NFA, "." + state.getName() + ".nfa.dot"), state.nfa);

                DFA.Builder builder = new DFA.Builder(errors, grammar, mask, state.nfa);
                DFA dfa = builder.build();
                // LOG.info(state + " -> " + dfa);
                state.dfa = dfa;

                dump(debugHandler.forTarget(STATE_DFA, "." + state.getName() + ".dfa.dot"), state.dfa);
            }

            for (Map.Entry<TokenModel, TokenSet> e : mask.entrySet()) {
                TokenModel token = e.getKey();
                StringBuilder buf = new StringBuilder();
                buf.append("Token '").append(token.getName()).append(" will never match because of higher priority token(s) ");
                boolean b = false;
                for (TokenModel t : e.getValue()) {
                    if (b)
                        buf.append(", ");
                    else
                        b = true;
                    buf.append("'").append(t.getName()).append("' at ").append(ErrorHandler.toLocationString(t.getLocation()));
                }
                if (!isOption(Option.ALLOWMASKEDTOKENS))
                    errors.addError(token.getLocation(), buf.toString());
                else
                    LOG.warn("{}: {}: {}", getName(), ErrorHandler.toLocationString(token.getLocation()), buf);
            }

            return EncodedStateMachine.forLexer(grammar, isOption(Option.INLINE_TABLES));
        } finally {
            LOG.info("{}: Building lexer took {}", getName(), stopwatch);
        }
    }

    protected void buildFunctions(@Nonnull GrammarModel grammar) throws IOException {
        CharSink sink = debugHandler.forTarget(FUNCTIONS, ".functions.txt");
        if (sink != null) {
            try (Writer writer = sink.openBufferedStream()) {
                FirstFunction firstFunction = new FirstFunction(grammar);
                for (CstProductionModel production : grammar.cstProductions.values())
                    writer.write("FIRST " + production.getName() + " -> " + firstFunction.apply(production) + "\n");
                for (CstProductionModel cstProductionRoot : grammar.getCstProductionRoots()) {
                    FollowFunction followFunction = new FollowFunction(grammar, cstProductionRoot, firstFunction);
                    for (CstProductionModel production : grammar.cstProductions.values())
                        writer.write("FOLLOW " + production.getName() + " -> " + followFunction.apply(production) + "\n");
                }
            }
        }
    }

    @Nonnull
    private static String rate(@Nonnull LRAutomaton automaton, @Nonnull Stopwatch stopwatch) {
        long ms = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        if (ms == 0)
            return "<inf>";
        return String.valueOf(automaton.getStates().size() * 1000L / ms);
    }

    @Nonnull
    protected LRAutomaton buildParserLr0(@Nonnull PolyglotExecutor executor, @Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot) throws IOException, InterruptedException, ExecutionException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        LR0ItemUniverse universe = new LR0ItemUniverse(grammar, cstProductionRoot);
        LOG.debug("{}: SLR Universe took {} and created {} items.", getName(), stopwatch, universe.size());
        LRAutomaton automaton = universe.build(executor);
        LOG.info("{}: Building SLR parser took {} and created {} states at {}/s.", getName(), stopwatch, automaton.getStates().size(), rate(automaton, stopwatch));

        dump(debugHandler.forTarget(AUTOMATON_LR0_DESC, ".lr0.txt"), debugHandler.forTarget(AUTOMATON_LR0, ".lr0.dot"), grammar, cstProductionRoot, automaton);

        LRConflict.Map conflicts = automaton.getConflicts();
        if (conflicts.isEmpty())
            return automaton;
        // This has to be INFO or gradle -i doesn't show it.
        if (isOption(Option.VERBOSE))
            LOG.info("{}: SLR conflicts are\n{}", getName(), conflicts);

        return automaton;
    }

    @Nonnull
    protected LRAutomaton buildParserLr1(@Nonnull PolyglotExecutor executor, @Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot) throws IOException, InterruptedException, ExecutionException {
        LRAutomaton automaton;

        for (int i = 0; true; i++) {
            Stopwatch stopwatch = Stopwatch.createStarted();

            // LOG.info("\n===\n=== Building LR(1) automaton, round {}\n===", i);
            LOG.info("{}: Building LR(1) parser, round {}.", getName(), i);
            LR1ItemUniverse universe = new LR1ItemUniverse(grammar, cstProductionRoot);
            LOG.debug("{}: LR(1) Universe took {} and created {} items.", getName(), stopwatch, universe.size());
            automaton = universe.build(executor);
            LOG.info("{}: Building LR(1) parser took {} and created {} states at {}/s.", getName(), stopwatch, automaton.getStates().size(), rate(automaton, stopwatch));

            dump(debugHandler.forTarget(AUTOMATON_LR1_DESC, ".lr1.v" + i + ".txt"), debugHandler.forTarget(AUTOMATON_LR1, ".lr1.v" + i + ".dot"), grammar, cstProductionRoot, automaton);

            LRConflict.Map conflicts = automaton.getConflicts();
            if (conflicts.isEmpty())
                return automaton;
            // This has to be INFO or gradle -i doesn't show it.
            if (isOption(Option.VERBOSE))
                LOG.info("{}: LR(1) conflicts are\n{}", getName(), conflicts);

            stopwatch = Stopwatch.createStarted();
            Inliner inliner = new Inliner(errors, grammar);
            boolean inliner_success = inliner.substitute(conflicts);
            LOG.info("{}: Inlining took {} and created {} substitutions.", getName(), stopwatch, inliner.getSubstitutions());
            dump(debugHandler.forTarget(GRAMMAR_SUBSTITUTED, ".substituted.v" + i + ".grammar"), grammar);
            // if (isOption(Option.VERBOSE)) LOG.debug("{}: Substitutions created {}", getName(), substitutions);
            if (!inliner_success) {
                errors.addError(null, "Inliner rejected inlining: Too many substitutions?");
                return automaton;
            }

            if (i >= 5) {
                errors.addError(null, getName() + ": Too many substitution attempts without success.");
                return automaton;
            }

            // Help the GC, so two universes or automata don't exist at the same time.
            universe = null;
            conflicts = null;
            automaton = null;
        }

        // NOTREACHED. Terminating the loop "normally" would return null here. :-(
    }

    @CheckForNull
    private LRDiagnoser newLRDiagnoser(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot) {
        try {
            Class<?> factoryType = Class.forName("org.anarres.polyglot.diagnoser.ChocoLRDiagnoser$Factory");
            LRDiagnoser.Factory factory = factoryType.asSubclass(LRDiagnoser.Factory.class).newInstance();
            return factory.newDiagnoser(grammar, cstProductionRoot, getOptions());
        } catch (ClassNotFoundException e) {
            LOG.info("{}: Failed to construct ChocoLRDiagnoser: {}", getName(), e);
        } catch (Exception e) {
            LOG.warn("{}: Failed to construct ChocoLRDiagnoser: {}", getName(), e, e);
        }
        return new AlgorithmicLRDiagnoser(grammar, cstProductionRoot);
    }

    @Nonnull
    protected void buildDiagnosis(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot, @Nonnull LRConflict.Map conflicts) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        if (isOption(Option.DIAGNOSIS)) {
            LRDiagnoser diagnoser = newLRDiagnoser(grammar, cstProductionRoot);
            StringBuilder buf = new StringBuilder();
            for (LRConflict conflict : conflicts.values()) {
                LRDiagnosis diagnosis = diagnoser.diagnose(conflict);
                diagnosis.toStringBuilder(buf);
                // LOG.debug("{}: LR conflict diagnosis is\n{}", getName(), diagnosis);
            }
            errors.addError(null, "Failed to generate an LR automaton:\n" + buf);
            LOG.info("{}: Diagnosing took {}", getName(), stopwatch);
        } else {
            errors.addError(null, "Failed to generate an LR automaton (DIAGNOSIS disabled):\n" + conflicts);
            // LOG.info("{}: Not diagnosing (DIAGNOSIS disabled). Final conflicts are:\n{}", getName(), conflicts);
        }
    }

    protected void buildOutputs(
            @Nonnull PolyglotExecutor executor,
            @Nonnull GrammarModel grammar,
            @CheckForNull EncodedStateMachine.Lexer lexerMachine,
            @Nonnull List<? extends EncodedStateMachine.Parser> parserMachines,
            Predicate<? super OutputLanguage> languages) throws IOException, InterruptedException, ExecutionException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info("{}: Writing output.", getName());
        OutputData data = new OutputData(getName(), grammar, lexerMachine, parserMachines, options);
        try {
            for (Map.Entry<OutputLanguage, File> e : outputDirs.entrySet()) {
                if (languages.apply(e.getKey())) {
                    LOG.info("{}: Writing output language {}", getName(), e.getKey());
                    OutputWriter writer = e.getKey().newOutputWriter(errors, e.getValue(), templates.row(e.getKey()), data);
                    writer.run(executor);
                }
            }
        } finally {
            executor.await();
        }
        LOG.info("{}: Writing output took {}", getName(), stopwatch);
    }

    /**
     * Executes the PolyglotEngine.
     *
     * If this method returns false, inspect {@link #getErrors()} for the reasons.
     *
     * @return true if execution succeeded, false otherwise.
     * @throws IOException if it all went wrong.
     * @throws InterruptedException if it was interrupted while it was thinking.
     * @throws ExecutionException if it all went very wrong indeed, asynchronously.
     */
    @CheckReturnValue
    public boolean run() throws IOException, InterruptedException, ExecutionException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        PolyglotExecutor executor = newExecutor();
        LOG.info("{}: Starting with {} threads and options {}.", getName(), executor.getParallelism(), getOptions());

        try {
            Start ast = buildParseTree();
            if (errors.isFatal() || ast == null)
                return false;
            GrammarModel grammar = buildModel(executor, ast);
            if (errors.isFatal())
                return false;
            // dump("Parsed grammar", ast);
            EncodedStateMachine.Lexer lexerMachine = buildLexer(grammar);
            if (errors.isFatal())
                return false;
            buildFunctions(grammar);
            if (errors.isFatal())
                return false;

            List<EncodedStateMachine.Parser> parserMachines = new ArrayList<>();
            for (CstProductionModel cstProductionRoot : grammar.getCstProductionRoots()) {
                AnnotationModel startAnnotation = cstProductionRoot.getAnnotation(AnnotationName.ParserStart);
                String machineName = startAnnotation == null ? "" : StartChecker.getMachineName(startAnnotation);

                LRAutomaton automaton;
                AUTOMATON:
                {
                    LRConflict.Map conflicts = null;

                    if (isOption(Option.SLR)) {
                        automaton = buildParserLr0(executor, grammar, cstProductionRoot);
                        conflicts = automaton.getConflicts();
                        if (conflicts.isEmpty())
                            break AUTOMATON;
                        if (errors.isFatal())
                            return false;
                        LOG.info("{}: SLR failed with {} conflicts; trying next strategy.", getName(), conflicts.size());
                    }

                    if (isOption(Option.LR1)) {
                        automaton = buildParserLr1(executor, grammar, cstProductionRoot);
                        conflicts = automaton.getConflicts();
                        if (conflicts.isEmpty())
                            break AUTOMATON;
                        if (errors.isFatal())
                            return false;
                        LOG.info("{}: LR(1) failed with {} conflicts; trying next strategy.", getName(), conflicts.size());
                    }

                    if (conflicts != null)
                        buildDiagnosis(grammar, cstProductionRoot, conflicts);
                    else
                        errors.addError(null, "Failed to build an LR automaton: No strategies enabled?");
                    return false;
                }
                parserMachines.add(EncodedStateMachine.forParser(machineName, automaton, cstProductionRoot, isOption(Option.INLINE_TABLES)));
            }

            buildOutputs(executor, grammar, lexerMachine, parserMachines,
                    Predicates.not(Predicates.equalTo(OutputLanguage.html)));
            if (errors.isFatal())
                return false;

        } finally {
            executor.await();
            LOG.info("{}: Overall took {}", getName(), stopwatch);
            executor.shutdown();
        }

        return true;
    }
}
