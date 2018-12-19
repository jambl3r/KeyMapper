package io.github.sds100.keymapper.Activities

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 10/12/2018.
 */

class AboutFragment : PreferenceFragmentCompat() {
    private val mChangelogPreference by lazy {
        findPreference(getString(R.string.action_changelog))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
    }
}