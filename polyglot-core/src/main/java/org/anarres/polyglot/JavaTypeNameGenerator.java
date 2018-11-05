package org.anarres.polyglot;

import com.google.common.io.BaseEncoding;
import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.xml.stream.events.Characters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTypeNameGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(JavaTypeNameGenerator.class);

    private static final Set<String> set = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>();
    private static final AtomicInteger id = new AtomicInteger();

    /*
        The current implementation is functional, but what I might prefer is to have an annotation
        for the tokens that are concerned.

        If we have an annotation or we can modify the grammar then we can simply throw an exception here
        if we detect that they are conflicts and have the dev fix it. Which I think would be a lot neater.
     */
    public static String generateJavaTypeName(@Nonnull String typeName) {
        return map.computeIfAbsent(typeName, key -> {
            final String keyLwr = key.toLowerCase();
            if (set.contains(keyLwr)) {
                final byte[] keyBytes = key.getBytes();
                final BitSet caseBits = new BitSet(keyBytes.length-1);
                // skip the first character as its assume to be a A/P/T prefix
                for (int it = 1; it < keyBytes.length; ++it) {
                    final int i = it-1;
                    caseBits.set(i, Character.isUpperCase(keyBytes[i]));
                }
                final String v = key + "__" + BaseEncoding.base32().omitPadding().encode(caseBits.toByteArray());
                LOG.warn("Found java type names differentiated only by case sensitivity, '" + key + "' has been renamed to '" + v + "'.");
                set.add(v.toLowerCase());
                return v;
            }
            else {
                set.add(keyLwr);
                return key;
            }
        });
    }
}
