# 「冲了吗」混淆规则
# 工程没有使用反射 / 序列化框架，默认规则足够；这里只保留行号，方便以后看崩溃栈。

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
