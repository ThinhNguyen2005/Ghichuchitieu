package com.notepay.data.preferences

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KnownBankAppsTest {

    @Test
    fun `automatic capture supports every TPBank alias`() {
        val aliases = KnownBankApps.equivalentPackages.getValue(KnownBankApps.TPBANK_PACKAGE)

        assertThat(aliases).isNotEmpty()
        assertThat(aliases.all(KnownBankApps::isSupported)).isTrue()
        assertThat(KnownBankApps.supportedPackages).containsExactlyElementsIn(aliases)
    }

    @Test
    fun `automatic capture rejects unverified banks and wallets`() {
        assertThat(KnownBankApps.isSupported("com.VCB")).isFalse()
        assertThat(KnownBankApps.isSupported("com.mservice.momotransfer")).isFalse()
        assertThat(KnownBankApps.isSupported("vn.com.techcombank.bb.app")).isFalse()
    }

    @Test
    fun `normalization expands a TPBank alias and removes unsupported packages`() {
        val normalized = KnownBankApps.normalizeSupportedPackages(
            setOf("com.tpbank", "com.VCB", "com.mservice.momotransfer"),
        )

        assertThat(normalized).containsExactlyElementsIn(KnownBankApps.supportedPackages)
    }

    @Test
    fun `normalization preserves an explicit empty selection`() {
        assertThat(KnownBankApps.normalizeSupportedPackages(emptySet())).isEmpty()
    }
}
