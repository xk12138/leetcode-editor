package com.shuzijun.leetcode.plugin.model;

import com.intellij.openapi.project.Project;
import com.shuzijun.leetcode.plugin.setting.PersistentConfig;
import com.shuzijun.leetcode.plugin.utils.MessageUtils;
import com.shuzijun.leetcode.plugin.utils.VelocityUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shuzijun
 */
public enum CodeTypeEnum {
    JAVA("Java", "java", ".java", "//", "/**\n%s\n*/"),
    PYTHON("Python", "python", ".py", "# ", "\"\"\"\n%s\n\"\"\""),
    CPP("C++", "cpp", ".cpp", "//", "/**\n%s\n*/") {

        final String mainTemplatePath = "/template/Cpp/main.cpp.template";
        final String leetcodeTemplatePath = "/template/Cpp/leetcode.h.template";
        final String leetcodeAbsoluteDirPath = File.separator + PersistentConfig.ROOT_DIR;
        final String headName = "leetcode.h";
        final String beforeBeginTemplate = "/template/Cpp/beforeBegin.template";
        final String afterEndTemplate = "/template/Cpp/afterEnd.template";

        @Override
        public void tryInitialize(Project project, Question question) {
            super.tryInitialize(project, question);

            Config config = PersistentConfig.getInstance().getInitConfig();

            // 检查main文件是否存在
            String mainPath = config.getFilePath() + File.separator + "main.cpp";
            File mainFile = new File(mainPath);
            if (!mainFile.exists()) {
                MessageUtils.getInstance(project).showWarnMsg("error", "没有在根目录下找到main.cpp文件，无法使用自动Debug模式");
                return;
            }

            // 检查cmakelist文件是否存在
            String cMakeListsPath = config.getFilePath() + File.separator + "CMakeLists.txt";
            File cMakeListsFile = new File(cMakeListsPath);
            if (!cMakeListsFile.exists()) {
                MessageUtils.getInstance(project).showWarnMsg("error", "没有在根目录下找到CMakeLists.txt文件，无法使用自动Debug模式");
                return;
            }

            // 创建leetcode根目录
            String leetcodeDirPath = config.getFilePath() + leetcodeAbsoluteDirPath;
            File leetcodeDir = new File(leetcodeDirPath);
            if (!leetcodeDir.exists()) {
                boolean result = leetcodeDir.mkdir();
                if (!result) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "没有成功创建leetcode文件夹");
                    return;
                }
            }

            // 开始创建leetcode.h
            String headPath = leetcodeDirPath + headName;
            File headFile = new File(headPath);
            if (!headFile.exists()) {
                try {
                    boolean result = headFile.createNewFile();
                    if (!result) {
                        MessageUtils.getInstance(project).showWarnMsg("error", "没有成功创建leetcode.h文件");
                        return;
                    }
                } catch (IOException e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "没有成功创建leetcode.h文件");
                    return;
                }
            }

            // 从模板中导入main.cpp
            {
                InputStream inputStream = this.getClass().getResourceAsStream(mainTemplatePath);
                if (inputStream == null) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "没有找到main.cpp的模板文件");
                    return;
                }
                byte[] bytes;
                try {
                    bytes = inputStream.readAllBytes();
                } catch (IOException e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "读取main.cpp的模板文件失败");
                    e.printStackTrace();
                    return;
                }
                try {
                    FileOutputStream mainOutputStream = new FileOutputStream(mainFile, false);
                    mainOutputStream.write(bytes);
                    mainOutputStream.close();
                } catch (Exception e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "写入main.cpp的模板文件失败");
                    e.printStackTrace();
                    return;
                }
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 从模板中导入leetcode.h
            {
                InputStream inputStream = this.getClass().getResourceAsStream(leetcodeTemplatePath);
                if (inputStream == null) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "没有找到leetcode.h的模板文件");
                    return;
                }
                byte[] bytes;
                try {
                    bytes = inputStream.readAllBytes();
                } catch (IOException e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "读取leetcode.h的模板文件失败");
                    e.printStackTrace();
                    return;
                }
                try {
                    FileOutputStream mainOutputStream = new FileOutputStream(headFile, false);
                    mainOutputStream.write(bytes);
                    mainOutputStream.close();
                } catch (Exception e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "写入leetcode.h的模板文件失败");
                    e.printStackTrace();
                    return;
                }
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 开始写入CMakeLists文件
            {
                String content = "";
                try {
                    InputStream inputStream = new FileInputStream(cMakeListsFile);
                    byte[] bytes = inputStream.readAllBytes();
                    content = new String(bytes);
                    inputStream.close();
                } catch (Exception e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "读取CMakeLists.txt文件失败");
                    e.printStackTrace();
                    return;
                }

                // TODO 这里应该能够很好的支持CMakeLists的加载和修改，但是我懒得写了
                // 解析CMakeLists文件，并对add_executable项进行修改，删除leetcode开头的其他项，只保留本次项
                StringBuilder result = new StringBuilder();
                String[] properties = content.split("\\)");
                for (String property : properties) {
                    if (!property.strip().startsWith("add_executable")) {
                        result.append(property);
                        if (property.replaceAll(" ", "").replaceAll("\t", "").replaceAll("\n", "").length() != 0) {
                            result.append(')');
                        }
                        continue;
                    }

                    StringBuilder addExecutable = new StringBuilder();
                    for (String executable : property.split(" ")) {
                        if (executable.strip().startsWith("leetcode/")) {
                            continue;
                        }

                        addExecutable.append(executable)
                                .append(' ');
                    }
                    addExecutable.append(' ')
                            .append(PersistentConfig.ROOT_DIR)
                            .append(headName)
                            .append(' ')
                            .append(PersistentConfig.getInstance().getAbsoluteFilePath().replace('\\', '/'))
                            .append(VelocityUtils.convert(config.getCustomFileName(), question))
                            .append(this.getSuffix())
                            .append(' ');

                    result.append(addExecutable)
                            .append(')');
                }

                try {
                    FileOutputStream mainOutputStream = new FileOutputStream(cMakeListsFile, false);
                    mainOutputStream.write(result.toString().getBytes());
                    mainOutputStream.close();
                } catch (Exception e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "写入CMakeLists.txt文件失败");
                    e.printStackTrace();
                    return;
                }
            }
        }

        @Override
        public String getBeforeBegin(Project project) {
            InputStream inputStream = this.getClass().getResourceAsStream(beforeBeginTemplate);
            if (inputStream == null) {
                MessageUtils.getInstance(project).showWarnMsg("error", "没有找到beforeBegin的模板文件");
                return "";
            }
            byte[] bytes;
            try {
                bytes = inputStream.readAllBytes();
            } catch (IOException e) {
                MessageUtils.getInstance(project).showWarnMsg("error", "读取beforeBegin的模板文件失败");
                e.printStackTrace();
                return "";
            }
            return new String(bytes);
        }

        @Override
        public String getAfterEnd(Project project) {
            InputStream inputStream = this.getClass().getResourceAsStream(afterEndTemplate);
            if (inputStream == null) {
                MessageUtils.getInstance(project).showWarnMsg("error", "没有找到afterEnd的模板文件");
                return "";
            }
            byte[] bytes;
            try {
                bytes = inputStream.readAllBytes();
            } catch (IOException e) {
                MessageUtils.getInstance(project).showWarnMsg("error", "读取afterEnd的模板文件失败");
                e.printStackTrace();
                return "";
            }
            return new String(bytes);
        }
    },
    PYTHON3("Python3", "python3", ".py", "# ", "\"\"\"\n%s\n\"\"\""),
    C("C", "c", ".c", "//", "/**\n%s\n*/") {

        final String mainTemplatePath = "/template/C/main.c.template";
        final String leetcodeTemplatePath = "/template/C/leetcode.h.template";
        final String leetcodeAbsoluteDirPath = File.separator + PersistentConfig.ROOT_DIR;
        final String headName = "leetcode.h";
        final String beforeBeginTemplate = "/template/C/beforeBegin.template";
        final String afterEndTemplate = "/template/C/afterEnd.template";

        @Override
        public void tryInitialize(Project project, Question question) {
            super.tryInitialize(project, question);

            Config config = PersistentConfig.getInstance().getInitConfig();

            // 检查main文件是否存在
            String mainPath = config.getFilePath() + File.separator + "main.c";
            File mainFile = new File(mainPath);
            if (!mainFile.exists()) {
                MessageUtils.getInstance(project).showWarnMsg("error", "没有在根目录下找到main.c文件，无法使用自动Debug模式");
                return;
            }

            // 检查cmakelist文件是否存在
            String cMakeListsPath = config.getFilePath() + File.separator + "CMakeLists.txt";
            File cMakeListsFile = new File(cMakeListsPath);
            if (!cMakeListsFile.exists()) {
                MessageUtils.getInstance(project).showWarnMsg("error", "没有在根目录下找到CMakeLists.txt文件，无法使用自动Debug模式");
                return;
            }

            // 创建leetcode根目录
            String leetcodeDirPath = config.getFilePath() + leetcodeAbsoluteDirPath;
            File leetcodeDir = new File(leetcodeDirPath);
            if (!leetcodeDir.exists()) {
                boolean result = leetcodeDir.mkdir();
                if (!result) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "没有成功创建leetcode文件夹");
                    return;
                }
            }

            // 开始创建leetcode.h
            String headPath = leetcodeDirPath + headName;
            File headFile = new File(headPath);
            if (!headFile.exists()) {
                try {
                    boolean result = headFile.createNewFile();
                    if (!result) {
                        MessageUtils.getInstance(project).showWarnMsg("error", "没有成功创建leetcode.h文件");
                        return;
                    }
                } catch (IOException e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "没有成功创建leetcode.h文件");
                    return;
                }
            }

            // 从模板中导入main.c
            {
                InputStream inputStream = this.getClass().getResourceAsStream(mainTemplatePath);
                if (inputStream == null) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "没有找到main.c的模板文件");
                    return;
                }
                byte[] bytes;
                try {
                    bytes = inputStream.readAllBytes();
                } catch (IOException e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "读取main.c的模板文件失败");
                    e.printStackTrace();
                    return;
                }
                try {
                    FileOutputStream mainOutputStream = new FileOutputStream(mainFile, false);
                    mainOutputStream.write(bytes);
                    mainOutputStream.close();
                } catch (Exception e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "写入main.c的模板文件失败");
                    e.printStackTrace();
                    return;
                }
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 从模板中导入leetcode.h
            {
                InputStream inputStream = this.getClass().getResourceAsStream(leetcodeTemplatePath);
                if (inputStream == null) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "没有找到leetcode.h的模板文件");
                    return;
                }
                byte[] bytes;
                try {
                    bytes = inputStream.readAllBytes();
                } catch (IOException e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "读取leetcode.h的模板文件失败");
                    e.printStackTrace();
                    return;
                }
                try {
                    FileOutputStream mainOutputStream = new FileOutputStream(headFile, false);
                    mainOutputStream.write(bytes);
                    mainOutputStream.close();
                } catch (Exception e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "写入leetcode.h的模板文件失败");
                    e.printStackTrace();
                    return;
                }
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 开始写入CMakeLists文件
            {
                String content = "";
                try {
                    InputStream inputStream = new FileInputStream(cMakeListsFile);
                    byte[] bytes = inputStream.readAllBytes();
                    content = new String(bytes);
                    inputStream.close();
                } catch (Exception e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "读取CMakeLists.txt文件失败");
                    e.printStackTrace();
                    return;
                }

                // TODO 这里应该能够很好的支持CMakeLists的加载和修改，但是我懒得写了
                // 解析CMakeLists文件，并对add_executable项进行修改，删除leetcode开头的其他项，只保留本次项
                StringBuilder result = new StringBuilder();
                String[] properties = content.split("\\)");
                for (String property : properties) {
                    if (!property.strip().startsWith("add_executable")) {
                        result.append(property);
                        if (property.replaceAll(" ", "").replaceAll("\t", "").replaceAll("\n", "").length() != 0) {
                            result.append(')');
                        }
                        continue;
                    }

                    StringBuilder addExecutable = new StringBuilder();
                    for (String executable : property.split(" ")) {
                        if (executable.strip().startsWith("leetcode/")) {
                            continue;
                        }

                        addExecutable.append(executable)
                                .append(' ');
                    }
                    addExecutable.append(' ')
                            .append(PersistentConfig.ROOT_DIR)
                            .append(headName)
                            .append(' ')
                            .append(PersistentConfig.getInstance().getAbsoluteFilePath().replace('\\', '/'))
                            .append(VelocityUtils.convert(config.getCustomFileName(), question))
                            .append(this.getSuffix())
                            .append(' ');

                    result.append(addExecutable)
                            .append(')');
                }

                try {
                    FileOutputStream mainOutputStream = new FileOutputStream(cMakeListsFile, false);
                    mainOutputStream.write(result.toString().getBytes());
                    mainOutputStream.close();
                } catch (Exception e) {
                    MessageUtils.getInstance(project).showWarnMsg("error", "写入CMakeLists.txt文件失败");
                    e.printStackTrace();
                    return;
                }
            }
        }

        @Override
        public String getBeforeBegin(Project project) {
            InputStream inputStream = this.getClass().getResourceAsStream(beforeBeginTemplate);
            if (inputStream == null) {
                MessageUtils.getInstance(project).showWarnMsg("error", "没有找到beforeBegin的模板文件");
                return "";
            }
            byte[] bytes;
            try {
                bytes = inputStream.readAllBytes();
            } catch (IOException e) {
                MessageUtils.getInstance(project).showWarnMsg("error", "读取beforeBegin的模板文件失败");
                e.printStackTrace();
                return "";
            }
            return new String(bytes);
        }

        @Override
        public String getAfterEnd(Project project) {
            InputStream inputStream = this.getClass().getResourceAsStream(afterEndTemplate);
            if (inputStream == null) {
                MessageUtils.getInstance(project).showWarnMsg("error", "没有找到afterEnd的模板文件");
                return "";
            }
            byte[] bytes;
            try {
                bytes = inputStream.readAllBytes();
            } catch (IOException e) {
                MessageUtils.getInstance(project).showWarnMsg("error", "读取afterEnd的模板文件失败");
                e.printStackTrace();
                return "";
            }
            return new String(bytes);
        }
    },
    CSHARP("C#", "csharp", ".cs", "//", "/**\n%s\n*/"),
    JAVASCRIPT("JavaScript", "javascript", ".js", "//", "/**\n%s\n*/"),
    RUBY("Ruby", "ruby", ".rb", "#", "=begin\n%s\n=end"),
    SWIFT("Swift", "swift", ".swift", "///", "/**\n%s\n*/"),
    GO("Go", "golang", ".go", "//", "/**\n%s\n*/"),
    SCALA("Scala", "scala", ".scala", "//", "/**\n%s\n*/"),
    KOTLIN("Kotlin", "kotlin", ".kt", "//", "/**\n%s\n*/"),
    RUST("Rust", "rust", ".rs", "//", "/**\n%s\n*/"),
    PHP("PHP", "php", ".php", "//", "/**\n%s\n*/"),
    BASH("Bash", "bash", ".sh", "#", ": '\n%s\n'"),
    MYSQL("MySQL", "mysql", ".sql", "#", "/**\n%s\n*/"),
    ORACLE("Oracle", "oraclesql", ".sql", "#", "/**\n%s\n*/"),
    MSSQLSERVER("MS SQL Server", "mssql", ".sql", "#", "/**\n%s\n*/"),
    TypeScript("TypeScript", "typescript", ".ts", "//", "/**\n%s\n*/"),
    ;


    private String type;
    private String langSlug;
    private String suffix;
    private String comment;
    private String multiLineComment;

    CodeTypeEnum(String type, String langSlug, String suffix, String comment, String multiLineComment) {
        this.type = type;
        this.langSlug = langSlug;
        this.suffix = suffix;
        this.comment = comment;
        this.multiLineComment = multiLineComment;
    }

    private static Map<String, CodeTypeEnum> MAP = new HashMap<String, CodeTypeEnum>();
    private static Map<String, CodeTypeEnum> LANGSLUGMAP = new HashMap<String, CodeTypeEnum>();

    static {
        for (CodeTypeEnum c : CodeTypeEnum.values()) {
            MAP.put(c.getType().toUpperCase(), c);
            LANGSLUGMAP.put(c.langSlug.toUpperCase(), c);
        }
    }

    public String getType() {
        return type;
    }

    public String getSuffix() {
        return suffix;
    }

    public static CodeTypeEnum getCodeTypeEnum(String type) {
        return MAP.get(type.toUpperCase());
    }

    public static CodeTypeEnum getCodeTypeEnumByLangSlug(String langSlug) {
        return LANGSLUGMAP.get(langSlug.toUpperCase());
    }

    public String getComment() {
        return comment;
    }

    public String getMultiLineComment() {
        return multiLineComment;
    }

    public void tryInitialize(Project project, Question question) {

    }

    public String getBeforeBegin(Project project) {
        return "";
    }

    public String getAfterEnd(Project project) {
        return "";
    }
}
