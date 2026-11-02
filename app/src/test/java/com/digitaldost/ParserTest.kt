package com.digitaldost

import com.digitaldost.engine.CommandParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserTest {
    @Test
    fun commandTable() {
        val failures = ParserCases.cases.filter { CommandParser.parse(it.first) != it.second }
        println("Parser accuracy: ${ParserCases.cases.size - failures.size}/${ParserCases.cases.size}")
        assertTrue(
            "Failed: " + failures.joinToString { "'${it.first}' -> ${CommandParser.parse(it.first)}" },
            failures.isEmpty()
        )
    }

    @Test
    fun cancelWords() {
        assertTrue(CommandParser.isCancel("nahi rehne do"))
        assertTrue(CommandParser.isCancel("cancel"))
        assertFalse(CommandParser.isCancel("I will reach home in 20 minutes"))
    }
}
