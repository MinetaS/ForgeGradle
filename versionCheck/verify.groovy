#!/usr/bin/env groovy

// Check file
File jsonFile;

if (args.length > 0) {
    jsonFile = new File(args[0])

    if (!jsonFile.exists()) {
        println "JSON file $jsonFile not found!"
        return 1
    }
}
else {
    jsonFile = new File("ForgeGradleVersion.json")

    if (!jsonFile.exists()) {
        println "json file (forgegradleversion.json) not found!"
        return 1
    }
}

println "analyzing file: $jsonFile"
println "----"
println ""

// Start verifying
import groovy.json.*

def result = new JsonSlurper().parse(jsonFile);
broke = false;
warned = false;

public void error(msg) {
    broke = true
    println "ERROR -- " + msg
}

public void warn(msg) {
    warned = true
    println "WARNING -- " + msg
}

def sorted = result.sort(false) { it.version }

if (sorted != result) {
    println "ERROR --"
    println "Versions are not sorted. Should be.. "
    println new JsonBuilder(sorted).toPrettyString()
    println " -- "
}

result.eachWithIndex { version, index ->
    def fieldBroke = false;

    ['status', 'docUrl', 'version', 'changes', 'bugs'].each { field ->
        if (result["${field}"] == null) {
            fieldBroke = true
            error "Version object with index ${index} is missing field ${field}"
        }
    }

    if (fieldBroke) {
        println "FIELDS BROKEN"
        System.exit 0
    }

    if (sorted[index] != version) {
        error "Index of '${version.version}' (${sorted[index]}) doesnt match vals actual index in the sorted list. Should be ${sorted.indexOf version.version}"
    }

    def status = version.status.toUpperCase();

    if (version.status != status) {
        error "Status '${version.status}' for version ${version.version} is not all upper case"
    }
    else if (!(["FINE", "BROKEN", "OUTDATED"].contains(status))) {
        error "Status '${version.status}' for version ${version.version} is not a valid status"
    }

    if (version.status == "OUTDATED") {
        if (!version.changes) {
            warn "'${version.version}' is marked 'OUTDATED' but there are no changes listed"
        }
    }
    else if (version.status == "BROKEN") {
        if (!version.bugs) {
            warn "'${version.version}' is marked 'BROKEN' but there are no bugs listed"
        }
    }
    else {
        if (index != result.versions.size() - 1) {
            error "'${version.version}' is not marked 'outdated' even though there is a newer version"
        }

        if (version.changes.size() <= 0) {
            warn "'${version.version}' is the newest version, but has no defined changes"
        }
    }

    if (!version.docUrl) {
        warn "'${version.version}' doesnt have a 'docUrl' defined!"
    }
    else if (!version.docUrl.startsWith('http://') && !version.docUrl.startsWith('https://')) {
        warn "'${version.version}' has a 'docUrl' that isnt an HTTP URL"
    }

    /// TODO: check the plugin-specific stuff
}

if (broke) {
    println()
    println "JSON has errors   :("
    return 1
}
else if (warned) {
    println()
    println "JSON has warnings   :|"
    return 1
}
else {
    println()
    println "JSON is perfect   :D"
    return 0
}
