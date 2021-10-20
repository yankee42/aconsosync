package com.github.yankee42.aconsosync

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.LocalDate

private val SAMPLE_DATE = LocalDate.of(2012, 1, 30)
private const val SAMPLE_ID = "id"
private const val SAMPLE_NAME = "name"
private val SAMPLE_DOCUMENT = Document(SAMPLE_ID, SAMPLE_NAME, SAMPLE_DATE)

class PathTemplateTest {
    @Test(dataProvider = "provide_templateString_document_expectedPath")
    fun getPath(templateString: String, expectedPath: String) {
        // execution
        val actual = PathTemplate(templateString).getPath(SAMPLE_DOCUMENT)

        // evaluation
        assertThat(actual.toString(), equalTo(expectedPath))
    }

    @Test
    fun getPath_replacesSlashesInAconsoFilename() {
        // execution
        val actual = PathTemplate("{name}").getPath(Document("id", "x/y", SAMPLE_DATE))

        // evaluation
        assertThat(actual.toString(), equalTo("x-y"))
    }

    @DataProvider
    fun provide_templateString_document_expectedPath(): Array<Array<Any>> {
        return arrayOf(
            // No variable
            arrayOf("constant", "constant"),

            // Single variable
            arrayOf("{id}", SAMPLE_ID),
            arrayOf("{name}", SAMPLE_NAME),
            arrayOf("{date}", "2012-01-30"),
            arrayOf("{date:DD.MM.YYYY}", "30.01.2012"),

            // combination of variable and text
            arrayOf("before{id}", "before$SAMPLE_ID"),
            arrayOf("{id}after", "${SAMPLE_ID}after"),
            arrayOf("before{id}after", "before${SAMPLE_ID}after"),

            // multiple variables
            arrayOf("{id}{name}{id}", "${SAMPLE_ID}${SAMPLE_NAME}${SAMPLE_ID}"),
            arrayOf("-{id}.{name}-", "-$SAMPLE_ID.$SAMPLE_NAME-"),

            // retains slashes so directories are possible
            arrayOf("dir/file", "dir/file")
        )
    }
}
