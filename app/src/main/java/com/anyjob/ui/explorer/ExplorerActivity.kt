package com.anyjob.ui.explorer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.anyjob.R
import com.anyjob.databinding.ActivityExplorerBinding

class ExplorerActivity : AppCompatActivity() {
    private val _binding: ActivityExplorerBinding by lazy {
        ActivityExplorerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        supportActionBar?.hide()

        val navigationItems = setOf(
            R.id.navigation_home,
            R.id.navigation_dashboard,
            R.id.navigation_profile
        )

        val navigationController = findNavController(R.id.explorer_fragments_container)
        val applicationBarConfiguration = AppBarConfiguration(navigationItems)
        setupActionBarWithNavController(navigationController, applicationBarConfiguration)
        _binding.navigationView.setupWithNavController(navigationController)
    }
}