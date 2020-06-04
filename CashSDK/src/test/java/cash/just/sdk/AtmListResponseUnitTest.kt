package cash.just.sdk

import cash.just.sdk.model.AtmMachine
import cash.just.sdk.model.isValidAmount
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class AtmListResponseUnitTest {
    @Test
    fun addition_isCorrect() {
      val atm = AtmMachine("","","",
          "","","","","", "",
          "20","100", "","")

        assert(atm.isValidAmount("25"))
        assert(atm.isValidAmount("20"))
        assert(atm.isValidAmount("100"))
        assert(atm.isValidAmount("99"))

        assertFalse(atm.isValidAmount("101"))
        assertFalse(atm.isValidAmount("10"))
        assertFalse(atm.isValidAmount(null))
    }
}
