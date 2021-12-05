package io.paly.esetsalaryslipparser.args

fun getPassword(args: Array<String>): String {
    return args.getOption("-p")
        .ifEmpty {
            print("Enter password: ")
            val password = System.console()?.readPassword()?.joinToString("")
                ?: readLine()
            password!!
        }
}

fun getSlipsDirectory(args: Array<String>): String {
    val option = args.getOption("--slips")
    if (option.isEmpty()) {
        throw Exception(
            "Please, specify slips directory.\n" +
                "Example: java -jar eset-salary-slips-1.0.jar --slips slips"
        )
    }
    return option
}

fun getDebug(args: Array<String>): Boolean {
    return args.getFlag("--debug")
}

private fun Array<String>.getOption(option: String): String {
    val index = indexOf(option)
    return if (index != -1 && index < size - 1) {
        get(index + 1)
    } else {
        ""
    }
}

private fun Array<String>.getFlag(option: String): Boolean {
    return indexOf(option) != -1
}
