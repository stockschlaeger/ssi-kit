package org.letstrust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configuration
import org.letstrust.cli.*
import org.apache.logging.log4j.core.config.LoggerConfig


data class CliConfig(var dataDir: String, val properties: MutableMap<String, String>, var verbose: Boolean)

private val logger = KotlinLogging.logger {}

class letstrust : CliktCommand(
    help = """LetsTrust CLI

        The LetsTrust CLI is a command line tool that allows you to onboard and use
        a SSI (Self-Sovereign-Identity) ecosystem. You can generate and register
        W3C Decentralized Identifiers (DIDs) including your public keys & service endpoints 
        as well as issue & verify W3C Verifiable credentials (VCs). 
        
        Example commands are:
        
        docker run -it letstrust key gen --algorithm Secp256k1

        docker run -it letstrust vc verify vc.json
        
        """
) {
    init {
        versionOption("1.0")
    }

    val dataDir: String by option("-d", "--data-dir", help = "Set data directory [./data].")
        .default("data")

    val config: Map<String, String> by option("-c", "--config", help = "Overrides a config key/value pair.").associate()
    val verbose: Boolean by option("-v", "--verbose", help = "Enables verbose mode.")
        .flag()

    override fun run() {

        val config = CliConfig(dataDir, HashMap(), verbose)
        for ((k, v) in this.config) {
            config.properties[k] = v
        }
        currentContext.obj = config

        println("Config loaded: ${config}\n")
    }
}


fun main(args: Array<String>) {

    val ctx: LoggerContext = LogManager.getContext(false) as LoggerContext
    val logConf: Configuration = ctx.getConfiguration()
    val loggerConfig: LoggerConfig = logConf.getLoggerConfig(LogManager.ROOT_LOGGER_NAME)

    args.forEach {
        if (it.contains("-v") || it.contains("--verbose")) {
            loggerConfig.level = Level.TRACE
        }
    }

    ctx.updateLoggers()

    logger.debug { "Let's Trust CLI started" }

    return letstrust()
        .subcommands(
            key().subcommands(gen(), listKeys(), exportKey()),
            did().subcommands(createDid(), resolveDid(), listDids()),
            vc().subcommands(issue(), verify()),
            auth()
        )
        //.main(arrayOf("-v", "-c", "mykey=myval", "vc", "-h"))
        //.main(arrayOf("vc", "verify", "vc.json"))
        .main(args)
}