/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.ExternalModel;
import org.anarres.polyglot.model.HelperModel;
import org.anarres.polyglot.model.Model;
import org.anarres.polyglot.model.PackageModel;
import org.anarres.polyglot.model.TokenModel;

/**
 *
 * @author shevek
 */
public class ModelMap<K> {

    /* pp */ final SetMultimap<K, PackageModel> packages = HashMultimap.create();
    /* pp */ final SetMultimap<K, ExternalModel> externals = HashMultimap.create();
    /* pp */ final SetMultimap<K, HelperModel> helpers = HashMultimap.create();
    /* pp */ final SetMultimap<K, TokenModel> tokens = HashMultimap.create();
    /* pp */ final SetMultimap<K, CstProductionModel> cstProductions = HashMultimap.create();
    /* pp */ final SetMultimap<K, CstAlternativeModel> cstAlternatives = HashMultimap.create();
    /* pp */ final SetMultimap<K, AstProductionModel> astProductions = HashMultimap.create();
    /* pp */ final SetMultimap<K, AstAlternativeModel> astAlternatives = HashMultimap.create();

    @Nonnull
    public Set<? extends K> getKeys() {
        Set<K> out = Sets.newHashSet();
        out.addAll(packages.keySet());
        out.addAll(externals.keySet());
        out.addAll(helpers.keySet());
        out.addAll(tokens.keySet());
        out.addAll(cstProductions.keySet());
        out.addAll(cstAlternatives.keySet());
        out.addAll(astProductions.keySet());
        out.addAll(astAlternatives.keySet());
        return out;
    }

    public void put(@Nonnull K key, @Nonnull Model m) {
        if (m instanceof PackageModel)
            packages.put(key, (PackageModel) m);
        else if (m instanceof ExternalModel)
            externals.put(key, (ExternalModel) m);
        else if (m instanceof HelperModel)
            helpers.put(key, (HelperModel) m);
        else if (m instanceof TokenModel)
            tokens.put(key, (TokenModel) m);
        else if (m instanceof CstProductionModel)
            cstProductions.put(key, (CstProductionModel) m);
        else if (m instanceof CstAlternativeModel)
            cstAlternatives.put(key, (CstAlternativeModel) m);
        else if (m instanceof AstProductionModel)
            astProductions.put(key, (AstProductionModel) m);
        else if (m instanceof AstAlternativeModel)
            astAlternatives.put(key, (AstAlternativeModel) m);
        else
            throw new IllegalArgumentException("Unknown model " + m.getClass());
    }

    public void put(@Nonnull Iterable<? extends K> keys, @Nonnull Model m) {
        for (K key : keys) {
            put(key, m);
        }
    }

    public <M extends Model> void put(@Nonnull M model, @Nonnull Function<? super M, ? extends Iterable<? extends K>> function) {
        put(function.apply(model), model);
    }

    public <M extends Model> void put(@Nonnull Iterable<? extends M> models, @Nonnull Function<? super M, ? extends Iterable<? extends K>> function) {
        for (M model : models)
            put(model, function);
    }

    public class ModelSet {

        private final K key;

        public ModelSet(@Nonnull K key) {
            this.key = key;
        }

        @Nonnull
        public K getKey() {
            return key;
        }

        @Nonnull
        public Set<PackageModel> getPackages() {
            return packages.get(key);
        }

        @Nonnull
        public Set<ExternalModel> getExternals() {
            return externals.get(key);
        }

        @Nonnull
        public Set<HelperModel> getHelpers() {
            return helpers.get(key);
        }

        @Nonnull
        public Set<TokenModel> getTokens() {
            return tokens.get(key);
        }

        @Nonnull
        public Set<CstProductionModel> getCstProductions() {
            return cstProductions.get(key);
        }

        @Nonnull
        public Set<CstAlternativeModel> getCstAlternatives() {
            return cstAlternatives.get(key);
        }

        @Nonnull
        public Set<AstProductionModel> getAstProductions() {
            return astProductions.get(key);
        }

        @Nonnull
        public Set<AstAlternativeModel> getAstAlternatives() {
            return astAlternatives.get(key);
        }
    }

    @Nonnull
    public ModelSet getValues(@Nonnull K key) {
        return new ModelSet(key);
    }
}
