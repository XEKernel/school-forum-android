// MathJax 基础配置
window.MathJax = {
  tex: { 
    inlineMath: [['$', '$'], ['\\(', '\\)']], 
    displayMath: [['$$', '$$'], ['\\[', '\\]']] 
  },
  options: { skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre', 'code'] },
  startup: { typeset: false },
  svg: { scale: 1.5 }
};
