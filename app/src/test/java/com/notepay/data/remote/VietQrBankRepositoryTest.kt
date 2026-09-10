package com.notepay.data.remote

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VietQrBankRepositoryTest {

    @Test
    fun `parseBundledBanks keeps transfer-supported banks and maps asset logo`() {
        val banks = parseBundledBanks(
            """
            {
              "banks": [
                {
                  "id": 43,
                  "bin": "970436",
                  "code": "VCB",
                  "shortName": "Vietcombank",
                  "name": "Ngân hàng TMCP Ngoại Thương Việt Nam",
                  "logoAssetPath": "vietqr/logos/VCB.png",
                  "transferSupported": true
                },
                {
                  "id": 99,
                  "bin": "999999",
                  "code": "TEST",
                  "shortName": "Test Bank",
                  "name": "Test Bank",
                  "logoAssetPath": "vietqr/logos/TEST.png",
                  "transferSupported": false
                }
              ]
            }
            """.trimIndent(),
        )

        assertThat(banks).hasSize(1)
        assertThat(banks.single().bin).isEqualTo("970436")
        assertThat(banks.single().logoUrl).isEqualTo("file:///android_asset/vietqr/logos/VCB.png")
    }

    @Test
    fun `parseBundledBanks skips entries without a BIN`() {
        val banks = parseBundledBanks(
            """{"banks":[{"id":1,"code":"EMPTY","transferSupported":true}]}""",
        )

        assertThat(banks).isEmpty()
    }
}
