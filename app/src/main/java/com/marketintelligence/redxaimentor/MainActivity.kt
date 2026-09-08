package com.marketintelligence.redxaimentor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marketintelligence.redxaimentor.features.behavioralbias.BehavioralBiasScreen
import com.marketintelligence.redxaimentor.features.crossasset.CrossAssetScreen
import com.marketintelligence.redxaimentor.features.evaluation.EvaluationScreen
import com.marketintelligence.redxaimentor.features.framework.FrameworkScreen
import com.marketintelligence.redxaimentor.features.governance.GovernanceScreen
import com.marketintelligence.redxaimentor.features.intelligence.IntelligenceScreen
import com.marketintelligence.redxaimentor.features.knowledge.KnowledgeScreen
import com.marketintelligence.redxaimentor.features.learning.LearningScreen
import com.marketintelligence.redxaimentor.features.marketregime.MarketRegimeScreen
import com.marketintelligence.redxaimentor.features.outcome.OutcomeScreen
import com.marketintelligence.redxaimentor.features.playbook.PlaybookScreen
import com.marketintelligence.redxaimentor.features.progression.ProgressionScreen
import com.marketintelligence.redxaimentor.features.risk.RiskScreen
import com.marketintelligence.redxaimentor.features.simulation.SimulationScreen
import com.marketintelligence.redxaimentor.features.stress.StressScreen
import com.marketintelligence.redxaimentor.ui.theme.REDXAIMentorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            REDXAIMentorTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(navController = navController, drawerState = drawerState)
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("REDX AI Mentor") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") { TrainingMentorApp() }
                composable("simulation") { SimulationScreen() }
                composable("evaluation") { EvaluationScreen() }
                composable("intelligence") { IntelligenceScreen() }
                composable("framework") { FrameworkScreen() }
                composable("governance") { GovernanceScreen() }
                composable("progression") { ProgressionScreen() }
                composable("market_regime") { MarketRegimeScreen() }
                composable("behavioral_bias") { BehavioralBiasScreen() }
                composable("cross_asset") { CrossAssetScreen() }
                composable("risk_discipline") { RiskScreen() }
                composable("outcome_attribution") { OutcomeScreen() }
                composable("learning_loop") { LearningScreen() }
                composable("stress_testing") { StressScreen() }
                composable("knowledge_versioning") { KnowledgeScreen() }
                composable("playbook_library") { PlaybookScreen() }
            }
        }
    }
}

@Composable
fun DrawerContent(navController: NavHostController, drawerState: DrawerState) {
    val scope = rememberCoroutineScope()
    val items = listOf(
        "home" to "Home",
        "simulation" to "Institutional Simulation Engine",
        "evaluation" to "Adaptive Evaluation & Certification",
        "intelligence" to "Explainable Mentor Intelligence",
        "framework" to "Unified Knowledge Framework",
        "governance" to "Governance & Audit Layer",
        "progression" to "Progression & Readiness Gate",
        "market_regime" to "Market Regime Intelligence",
        "behavioral_bias" to "Behavioral Bias Detection",
        "cross_asset" to "Cross-Asset Context Engine",
        "risk_discipline" to "Risk Discipline Engine",
        "outcome_attribution" to "Outcome Attribution Engine",
        "learning_loop" to "Continuous Learning Loop",
        "stress_testing" to "Scenario Stress Testing",
        "knowledge_versioning" to "Knowledge Versioning & Replay",
        "playbook_library" to "Institutional Playbook Library"
    )

    ModalDrawerSheet {
        items.forEach { (route, title) ->
            NavigationDrawerItem(
                label = { Text(title) },
                selected = false,
                onClick = {
                    navController.navigate(route)
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    }
}

@Composable
fun TrainingMentorApp(modifier: Modifier = Modifier) {
}
