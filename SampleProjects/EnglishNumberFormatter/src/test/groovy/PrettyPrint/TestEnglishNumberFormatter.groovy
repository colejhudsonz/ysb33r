package PrettyPrint

import static org.junit.Assert.*
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class TestEnglishNumberFormatter {

    void _assert( BigInteger value, String expected ) {
        assertEquals "Tested ${} == '${expected}'", expected,EnglishNumberFormatter.fmt(value)    
    }
    
    @Test
    void upToTwentyPrintsFixedName() {
        _assert 0,  "nil"
        _assert 1,  "one"
        _assert 9,  "nine"
        _assert 11, "eleven"
        _assert 12, "twelve"
        _assert 19, "nineteen"
    } 
    
    @Test
    void anyMultipleOfTenUpToNinetyPrintsFixedName() {
         _assert 20, "twenty"  
         _assert 30, "thirty"  
         _assert 90, "ninety"  
    }
    
    @Test
    void anyNonMultipleOfTenBelowHundredIsOfForm_BASENAME_MODULO() {
        _assert 21, "twenty one"
        _assert 99, "ninety nine"
    }
    
    @Test 
    void anyMultipleofHundredBelowThousandIsOfForm_MULTIPLIER_HUNDRED() {
        _assert 100, "one hundred"
        _assert 500, "five hundred"
        _assert 900, "nine hundred"        
    }
    
    @Test
    void andIsUsedtoJoinHundredOfFrom_MULTIPLIER_HUNDRED_AND_MODULO() {
        _assert 105, "one hundred and five"
        _assert 598, "five hundred and ninety eight"
    }
    
    @Test
    void anyMultipleOfThousandBelowHundredThousandIsOfForm_MULTIPLIER_THOUSAND() {
        _assert   1_000, "one thousand"
        _assert  10_000, "ten thousand"
        _assert  15_000, "fifteen thousand"
        _assert  78_000, "seventy eight thousand"
        _assert  90_000, "ninety thousand"
    }
    
    @Test
    void andIsUsedtoJoinThousandOfFrom_MULTIPLIER_THOUSAND_AND_MODULO() {
        _assert 3003, "three thousand and three"
        _assert 4056, "four thousand and fifty six"
        _assert 100_000, "one hundred thousand"
        _assert 900_000, "nine hundred thousand"        
    }
    
    @Test 
    void andIsNotUsedToJoinThousandToHundred() {
        _assert 6500, "six thousand five hundred"
        _assert 7623, "seven thousand six hundred and twenty three"
    }
    
    @Test
    void anyMultipleOfMillionIsOfForm_MULTIPLIER_MILLION() {
        _assert   1_000_000, "one million"
        _assert 900_000_000, "nine hundred million"    
    }
    
    @Test
    void andIsOnlyUsedToJoinLessThanHundredValuesToHundredsAndThousandsAndMillions() {
        _assert     120_023, "one hundred and twenty thousand and twenty three"
        _assert     323_423, "three hundred and twenty three thousand four hundred and twenty three"
        _assert   2_000_005, "two million and five"
        _assert   3_000_025, "three million and twenty five"
        _assert  55_000_333, "fifty five million three hundred and thirty three"
        _assert  78_004_000, "seventy eight million four thousand"
        _assert  78_004_344, "seventy eight million four thousand three hundred and fourty four"
        _assert  78_123_456, "seventy eight million one hundred and twenty three thousand four hundred and fifty six"
        _assert 123_000_010, "one hundred and twenty three million and ten"
        _assert 234_010_010, "two hundred and thirty four million ten thousand and ten"
        _assert 234_110_023, "two hundred and thirty four million one hundred and ten thousand and twenty three"
    }
    
    @Test(expected=java.lang.NumberFormatException)
    void decimalsNotAllowed () {
        EnglishNumberFormatter.fmt("99.99")
    } 

    @Test(expected=java.lang.NumberFormatException)
    void negativesNotAllowed () {
        EnglishNumberFormatter.fmt(-1G)
    }

    @Test 
    void maxNumberPrints() {
        _assert 999_999_999, "nine hundred and ninety nine million nine hundred and ninety nine thousand nine hundred and ninety nine"
    }
    
    @Test(expected=java.lang.NumberFormatException)
    void maxNumberIs999999999() {
        EnglishNumberFormatter.fmt(1_000_000_000G)
    }
}
