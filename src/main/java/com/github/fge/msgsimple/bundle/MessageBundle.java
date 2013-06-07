/*
 * Copyright (c) 2013, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.fge.msgsimple.bundle;

import com.github.fge.Frozen;
import com.github.fge.msgsimple.locale.LocaleUtils;
import com.github.fge.msgsimple.provider.MessageSourceProvider;
import com.github.fge.msgsimple.serviceloader.MessageBundles;
import com.github.fge.msgsimple.serviceloader.MsgSimpleMessageBundle;
import com.github.fge.msgsimple.source.MessageSource;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingFormatArgumentException;

/**
 * Base abstract class for message bundles
 *
 * <p>A bundle is a list of {@link MessageSourceProvider}s. When a message is
 * looked up, providers are queried in order for a {@link MessageSource} for
 * that locale; if a source is found, it is queried for the message.</p>
 *
 * <p>When a method is used which does not take a {@link Locale} as an argument,
 * the default locale is used (obtained via {@link Locale#getDefault()}). If no
 * match is found for the locale, the next, more general locale is tried.</p>
 *
 * <p>Finally, if no match was found for any provider/source, the key itself is
 * returned.</p>
 *
 * <p>You cannot instantiate this class directly: use {@link #newBuilder()} to
 * obtain a builder, then that builder's {@link MessageBundleBuilder#freeze()}
 * method to obtain the bundle; alternatively, you can reuse an existing bundle
 * and {@link #thaw()} it, modify it and freeze it again.</p>
 *
 * @see LocaleUtils#getApplicable(Locale)
 * @see MessageSourceProvider
 */
@ThreadSafe
public final class MessageBundle
    implements Frozen<MessageBundleBuilder>
{
    private static final MessageBundle BUNDLE
        = MessageBundles.forClass(MsgSimpleMessageBundle.class);

    final List<MessageSourceProvider> providers
        = new ArrayList<MessageSourceProvider>();

    /**
     * Create a new, empty builder for a bundle
     *
     * @return a builder
     */
    public static MessageBundleBuilder newBuilder()
    {
        return new MessageBundleBuilder();
    }

    MessageBundle(final MessageBundleBuilder builder)
    {
        providers.addAll(builder.providers);
    }

    /**
     * Get a message for the given key and locale
     *
     * @param locale the locale
     * @param key the key
     * @return a matching message if found; the key itself if no message is
     * found
     * @throws NullPointerException either the key or the locale is null
     */
    public String getMessage(final Locale locale, final String key)
    {
        if (key == null)
            throw new NullPointerException(BUNDLE.getMessage("query.nullKey"));
        if (locale == null)
            throw new NullPointerException(BUNDLE.getMessage("query.nullLocale"));

        String ret;
        MessageSource source;

        for (final Locale l: LocaleUtils.getApplicable(locale))
            for (final MessageSourceProvider provider: providers) {
                source = provider.getMessageSource(l);
                if (source == null)
                    continue;
                ret = source.getKey(key);
                if (ret != null)
                    return ret;
            }

        // No source found which has the key... Return the key itself.
        return key;
    }

    /**
     * Return a message for a given key, using the JVM's current locale
     *
     * @param key the key
     * @return a matching message if found; the key itself if no message is
     * found
     * @throws NullPointerException key is null
     * @see Locale#getDefault()
     * @see Locale#setDefault(Locale)
     */
    public String getMessage(final String key)
    {
        return getMessage(Locale.getDefault(), key);
    }

    /**
     * Get a locale-dependent {@code printf()}-like formatted message
     *
     * <p>This is essentially a call to {@link String#format(Locale, String,
     * Object...)}.</p>
     *
     * <p>There is however one important difference: while {@code
     * String.format()} will throw a {@link MissingFormatArgumentException} if
     * an argument is missing, this method returns the format itself (or the
     * key, like {@link #getMessage(Locale, String)}, if no match was found for
     * that locale/key pair).</p>
     *
     * @param locale the locale
     * @param key the key
     * @param params the format parameters
     * @return the formatted message
     */
    public String printf(final Locale locale, final String key,
        final Object... params)
    {
        final String format = getMessage(locale, key);
        try {
            return String.format(locale, format, params);
        } catch (MissingFormatArgumentException ignored) {
            return format;
        }
    }

    /**
     * Get a {@code printf()}-like message with the default locale
     *
     * <p>This calls {@link #printf(Locale, String, Object...)} with {@link
     * Locale#getDefault()} as the first argument.</p>
     *
     * @param key the key
     * @param params the format parameters
     * @return the formatted message
     */
    public String printf(final String key, final Object... params)
    {
        return printf(Locale.getDefault(), key,params);
    }

    @Override
    public MessageBundleBuilder thaw()
    {
        return new MessageBundleBuilder(this);
    }

    private static String quoteFormat(final String input)
    {
        final StringBuilder sb = new StringBuilder();

        for (final char c: input.toCharArray()) {
            sb.append(c);
            if (c == '\'')
                sb.append(c);
        }

        return sb.toString();
    }
}
