package com.schoolforum.app.utils;

import io.noties.prism4j.annotations.PrismBundle;

/**
 * Prism4j 语言包注册（配合 prism4j-bundler 注解处理器）
 *
 * includeAll = true：将 prism4j-bundler 依赖自带资源中的全部语言
 * （java/javascript/python/c/cpp/go/kotlin/sql/yaml/css/markdown/latex 等 25 种，
 * 语言文件内的 @Aliases 别名如 js/html/xml 由 bundler 自动解析）打包进生成的 GrammarLocator。
 *
 * grammarLocatorClassName 以 "." 开头 → 生成到本类同包（com.schoolforum.app.utils），
 * 即编译期生成 com.schoolforum.app.utils.Prism4jBundler，运行时直接
 * new Prism4jBundler() 作为 Prism4j 的 GrammarLocator 使用。
 */
@PrismBundle(
        includeAll = true,
        grammarLocatorClassName = ".Prism4jBundler"
)
public class ForumPrismBundle {

    private ForumPrismBundle() {}
}
