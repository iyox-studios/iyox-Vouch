package com.iyox.vouch.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainView(viewModel: HomeViewModel = viewModel()) {
    val vouchData by viewModel.vouchData.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(initial = false)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { viewModel.fetchVouchData() }) {
            Text("Get Data")
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            isLoading -> {
                LoadingIndicator()
            }
            vouchData != null -> {
                Text("Amount: ${vouchData?.amount}")
                Text("Description: ${vouchData?.description}")
            }
        }
    }
}

