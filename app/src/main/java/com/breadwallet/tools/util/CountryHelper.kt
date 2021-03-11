package com.breadwallet.tools.util

import com.breadwallet.entities.Country
import java.util.Locale

/** Litewallet
 * Created by Mohamed Barry on 8/5/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
object CountryHelper {
    val countries: List<Country> = Locale.getISOCountries().map {
        with(Locale("", it)) {
            Country(displayCountry, it)
        }
    }.sortedWith(compareBy { it.name }).toList()

    val usaCountry = Country("United States", "US")
}
