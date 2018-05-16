import android.util.Log;

import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.wallet.WalletsMaster;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/3/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class PaperKeyTests {

    private static final String TAG = PaperKeyTests.class.getName();
    public static final String PAPER_KEY_JAP = "こせき　ぎじにってい　けっこん　せつぞく　うんどう　ふこう　にっすう　こせい　きさま　なまみ　たきび　はかい";//japanese
    public static final String PAPER_KEY_ENG = "stick sword keen   afraid smile sting   huge relax nominee   arena area gift ";//english
    public static final String PAPER_KEY_FRE = "vocation triage capsule marchand onduler tibia illicite entier fureur minorer amateur lubie";//french
    public static final String PAPER_KEY_SPA = "zorro turismo mezcla nicho morir chico blanco pájaro alba esencia roer repetir";//spanish
    public static final String PAPER_KEY_CHI = "怨 贪 旁 扎 吹 音 决 廷 十 助 畜 怒";//chinese


//    @Test
//    public void testWordsValid() {
//
//        List<String> list = getAllWords();
//        assertThat(list.size(), is(10240));
//
//        assertThat(isValid(PAPER_KEY_JAP, list), is(true));
//        assertThat(isValid(PAPER_KEY_ENG, list), is(true));
//        assertThat(isValid(PAPER_KEY_FRE, list), is(true));
//        assertThat(isValid(PAPER_KEY_SPA, list), is(true));
//        assertThat(isValid(PAPER_KEY_CHI, list), is(true));
//    }

    @Test
    public void testPaperKeyValidation() {
        List<String> list = getAllWords();
        assertThat(list.size(), is(10240));
    }

    private List<String> getAllWords() {
        List<String> result = new ArrayList<>();
        List<String> names = new ArrayList<>();
        names.add("en-BIP39Words.txt");
        names.add("es-BIP39Words.txt");
        names.add("fr-BIP39Words.txt");
        names.add("ja-BIP39Words.txt");
        names.add("zh-BIP39Words.txt");

        for (String fileName : names) {
            InputStream in = null;
            try {
                in = getClass().getResourceAsStream(fileName);
                String str = IOUtils.toString(in);
                String lines[] = str.split("\\r?\\n");
                result.addAll(Arrays.asList(lines));
            } catch (IOException e) {
                Log.e(TAG, "getAllWords: " + fileName + ", ", e);
            } finally {
                if (in != null) try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        List<String> cleanList = new ArrayList<>();
        for (String s : result) {
            String cleanWord = Bip39Reader.cleanWord(s);
            cleanList.add(cleanWord);
        }
        assertThat(cleanList.size(), is(10240));
        return cleanList;
    }

//    private boolean isValid(String phrase, List<String> words) {
//
//        return WalletsMaster.getInstance(null).validateRecoveryPhrase((String[]) words.toArray(), phrase);
//    }

}
