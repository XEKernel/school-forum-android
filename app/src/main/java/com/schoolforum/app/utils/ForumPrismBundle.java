package com.schoolforum.app.utils;

import io.noties.prism4j.annotations.Aliases;
import io.noties.prism4j.annotations.PrismBundle;
import io.noties.prism4j.languages.Prism_brainfuck;
import io.noties.prism4j.languages.Prism_c;
import io.noties.prism4j.languages.Prism_clike;
import io.noties.prism4j.languages.Prism_clojure;
import io.noties.prism4j.languages.Prism_cpp;
import io.noties.prism4j.languages.Prism_csharp;
import io.noties.prism4j.languages.Prism_css;
import io.noties.prism4j.languages.Prism_css_extras;
import io.noties.prism4j.languages.Prism_dart;
import io.noties.prism4j.languages.Prism_git;
import io.noties.prism4j.languages.Prism_go;
import io.noties.prism4j.languages.Prism_groovy;
import io.noties.prism4j.languages.Prism_java;
import io.noties.prism4j.languages.Prism_javascript;
import io.noties.prism4j.languages.Prism_json;
import io.noties.prism4j.languages.Prism_kotlin;
import io.noties.prism4j.languages.Prism_latex;
import io.noties.prism4j.languages.Prism_makefile;
import io.noties.prism4j.languages.Prism_markdown;
import io.noties.prism4j.languages.Prism_markup;
import io.noties.prism4j.languages.Prism_python;
import io.noties.prism4j.languages.Prism_scala;
import io.noties.prism4j.languages.Prism_sql;
import io.noties.prism4j.languages.Prism_swift;
import io.noties.prism4j.languages.Prism_yaml;

/**
 * Prism4j 语言包注册（配合 prism4j-bundler 注解处理器）
 * 编译期扫描此注解生成 io.noties.prism4j.bundler.Prism4jBundler，
 * 运行时 new Prism4jBundler() 即可作为 GrammarLocator 使用
 */
@PrismBundle(
        include = {
                Prism_brainfuck.class,
                Prism_c.class,
                Prism_clike.class,
                Prism_clojure.class,
                Prism_cpp.class,
                Prism_csharp.class,
                Prism_css.class,
                Prism_css_extras.class,
                Prism_dart.class,
                Prism_git.class,
                Prism_go.class,
                Prism_groovy.class,
                Prism_java.class,
                Prism_javascript.class,
                Prism_json.class,
                Prism_kotlin.class,
                Prism_latex.class,
                Prism_makefile.class,
                Prism_markdown.class,
                Prism_markup.class,
                Prism_python.class,
                Prism_scala.class,
                Prism_sql.class,
                Prism_swift.class,
                Prism_yaml.class
        },
        aliases = {
                @Aliases(ids = {"js", "jsx", "node"}, value = Prism_javascript.class),
                @Aliases(ids = {"html", "xml", "svg", "vue"}, value = Prism_markup.class),
                @Aliases(ids = {"py", "py3"}, value = Prism_python.class),
                @Aliases(ids = {"c++", "cc"}, value = Prism_cpp.class),
                @Aliases(ids = {"c#", "cs"}, value = Prism_csharp.class),
                @Aliases(ids = {"kt", "kts"}, value = Prism_kotlin.class),
                @Aliases(ids = {"yml"}, value = Prism_yaml.class),
                @Aliases(ids = {"md", "mkdown"}, value = Prism_markdown.class),
                @Aliases(ids = {"tex"}, value = Prism_latex.class),
                @Aliases(ids = {"sh", "shell", "bash"}, value = Prism_makefile.class)
        }
)
public class ForumPrismBundle {

    private ForumPrismBundle() {}
}
