/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.diagnoser;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.lr.LR0Item;
import org.anarres.polyglot.lr.LR0ItemUniverse;
import org.anarres.polyglot.lr.LRAction;
import org.anarres.polyglot.lr.LRConflict;
import org.anarres.polyglot.lr.LRDiagnosis;
import org.anarres.polyglot.lr.LRItem;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.GrammarModel;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.cstrs.GraphConstraintFactory;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.GraphStrategyFactory;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.trace.Chatterbox;
import org.chocosolver.solver.variables.GraphVarFactory;
import org.chocosolver.solver.variables.IDirectedGraphVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class ChocoLRDiagnoser implements LRDiagnoser {

    private static final Logger LOG = LoggerFactory.getLogger(ChocoLRDiagnoser.class);

    public static class Factory implements LRDiagnoser.Factory {

        @Override
        public LRDiagnoser newDiagnoser(GrammarModel grammar, Set<? extends Option> options) {
            return new ChocoLRDiagnoser(grammar);
        }
    }

    private final GrammarModel grammar;
    private final LR0ItemUniverse universe;
    // private final FirstFunction firstFunction;

    public ChocoLRDiagnoser(GrammarModel grammar) {
        this.grammar = grammar;
        this.universe = new LR0ItemUniverse(grammar);
        // this.firstFunction = new FirstFunction(grammar);
    }

    @Nonnull
    private LR0Item toLR0Item(@Nonnull LRItem item) {
        LR0Item item0 = universe.findZeroItem(item.getProductionAlternative());
        LR0Item out = universe.getItemByIndex(item0.getIndex() + item.getPosition());
        Preconditions.checkState(item.getProductionAlternative() == out.getProductionAlternative(), "Bad production alternative");
        Preconditions.checkState(item.getPosition() == out.getPosition(), "Bad position");
        return out;
    }

    @Override
    public LRDiagnosis diagnose(@Nonnull LRConflict conflict) {
        LOG.info("LRConflict is\n" + conflict);

        LRDiagnosis out = new LRDiagnosis(conflict);

        for (Map.Entry<? extends LRAction, ? extends LRItem> e : conflict.getItems().entrySet()) {
            LRAction key = e.getKey();
            LRItem item1 = e.getValue();
            LR0Item item0 = toLR0Item(item1);

            Solver solver = new Solver() {
                @Override
                public ESat isSatisfied() {
                    LOG.warn("isSatisfied called", new Exception());
                    // Due to non-implementation in the Nocycle constraint, we have to:
                    return ESat.TRUE;
                }
            };

            // SearchMonitorFactory.limitTime(solver, 4000);
            // Chatterbox.showDecisions(solver);
            Chatterbox.showSolutions(solver);
            Chatterbox.showStatistics(solver);

            // These cannot be 'true' otherwise we can't remove from them.
            DirectedGraph glb = new DirectedGraph(solver, universe.size(), SetType.BITSET, false);
            DirectedGraph gub = new DirectedGraph(solver, universe.size(), SetType.BITSET, false);

            // Vertices
            for (LR0Item item : universe.getItems()) {
                LOG.info("Adding [" + item.getIndex() + "] " + item);
                gub.addNode(item.getIndex());
            }

            // Edges
            for (LR0Item item : universe.getItems()) {
                CstProductionSymbol symbol = item.getSymbol();
                if (symbol == null)
                    continue;

                {
                    // A shift rule or a completed reduce.
                    LR0Item subitem = universe.getItemByIndex(item.getIndex() + 1);
                    LOG.info("Edge " + item + " -> " + subitem);
                    gub.addArc(item.getIndex(), item.getIndex() + 1);
                }

                if (symbol instanceof CstProductionModel) {
                    // A sub-production/reduce rule.
                    CstProductionModel cstProduction = (CstProductionModel) symbol;
                    for (CstAlternativeModel cstAlternative : cstProduction.alternatives.values()) {
                        LR0Item subitem = universe.findZeroItem(cstAlternative);
                        LOG.info("Edge " + item + " -> " + subitem);
                        gub.addArc(item.getIndex(), subitem.getIndex());
                    }
                }
            }

            // The root production.
            glb.addNode(0);
            glb.addNode(item0.getIndex());
            LOG.info("Searching for path to " + item0);
            // LOG.info("Searching gub " + gub);
            // LOG.info("Searching glb " + glb);

            IDirectedGraphVar var = GraphVarFactory.directed_graph_var("grammar", glb, gub, solver);
            LOG.info("Searching in " + var);

            solver.set(GraphStrategyFactory.lexico(var));

            // solver.post(GraphConstraintFactory.directed_tree(var, 0));
            // solver.post(GraphConstraintFactory.max_out_degrees(var, 1));
            // solver.post(GraphConstraintFactory.max_in_degrees(var, 1));
            // int[] nChildren = new int[var.getNbMaxNodes()];
            // for (int i = 0; i < nChildren.length; i++)
            // nChildren[i] = 1;
            // for (LRItem item1 : conflict.getItems().values()) {
            // LR0Item item0 = toLR0Item(item1);
            // LOG.info(item1 + " => " + item0);
            // nChildren[item0.getIndex()] = 0;
            // }
            // solver.post(GraphConstraintFactory.max_out_degrees(var, nChildren));
            solver.post(GraphConstraintFactory.path(var, 0, item0.getIndex()));

            IntVar nNodes = GraphVarFactory.nb_nodes(var);
            // GraphConstraintFactory.nodes_channeling(var, nodes);

            if (true) {
                solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, nNodes);
            } else {
                boolean success = solver.findSolution();
            }

            // solver.set(GraphStrategyFactory.lexico(var));
            // solver.findAllSolutions();
            // Preconditions.checkState(success, "Failed to find a diagnosis.");
            LOG.info("Graph is " + var);

            Solution solution = solver.getSolutionRecorder().getLastSolution();
            LOG.info("Solution is " + solution);
            try {
                solution.restore();
            } catch (ContradictionException ex) {
                throw Throwables.propagate(ex);
            }
            LOG.info("Variables are " + Arrays.toString(solver.getVars()));
            LOG.info(String.format("[STATISTICS {%s]", solver.getMeasures().toOneLineString()));

            break;
        }

        return out;
    }
}
