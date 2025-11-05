package kolskypavel.ardfmanager.ui.results

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kolskypavel.ardfmanager.BottomNavDirections
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kolskypavel.ardfmanager.databinding.FragmentResultsBinding
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel
import kolskypavel.ardfmanager.ui.races.RaceEditDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResultsFragment : Fragment() {

    private var _binding: FragmentResultsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val selectedRaceViewModel: SelectedRaceViewModel by activityViewModels()
    private val dataProcessor = DataProcessor.get()

    private lateinit var resultsSwipeLayout: SwipeRefreshLayout
    private lateinit var resultsToolbar: Toolbar
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var resultsServiceMenuItem: MenuItem

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        resultsRecyclerView.adapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resultsSwipeLayout = view.findViewById(R.id.results_swipe_layout)
        resultsToolbar = view.findViewById(R.id.results_toolbar)
        resultsRecyclerView = view.findViewById(R.id.results_recycler_view)
        resultsToolbar.inflateMenu(R.menu.fragment_menu_result)
        resultsServiceMenuItem = resultsToolbar.menu.findItem(R.id.result_menu_results_service)
        resultsToolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener setFragmentMenuActions(it)
        }

        // Set the toolbar as the action bar
        selectedRaceViewModel.race.observe(viewLifecycleOwner) { race ->
            resultsToolbar.title = race?.name
            race?.let { resultsToolbar.subtitle = dataProcessor.raceTypeToString(it.raceType) }
        }

        resultsSwipeLayout.setOnRefreshListener {
            selectedRaceViewModel.getCurrentRace()
                ?.let { race -> selectedRaceViewModel.updateResultsByRace(race.id) }
            resultsSwipeLayout.isRefreshing = false
        }

        // Set results service icon
        selectedRaceViewModel.resultService.observe(viewLifecycleOwner) { data ->
            if (data != null && data.resultService?.enabled == true) {
                resultsServiceMenuItem.icon =
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_result_service_running,
                        null
                    )
            } else {
                resultsServiceMenuItem.icon =
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_result_service_stopped,
                        null
                    )
            }
        }

        setResultListener()
        setBackButton()
        setRecyclerViewAdapter()
    }

    private fun setFragmentMenuActions(menuItem: MenuItem): Boolean {

        when (menuItem.itemId) {
            R.id.result_menu_share_results -> {
                findNavController().navigate(ResultsFragmentDirections.exportResults())
            }

            R.id.result_menu_results_service -> {
                selectedRaceViewModel.getCurrentRace()?.let { race ->
                    findNavController().navigate(ResultsFragmentDirections.openResultService(race))
                }
            }

            R.id.result_menu_print_results -> {
                selectedRaceViewModel.getCurrentRace()?.let { race ->
                    CoroutineScope(Dispatchers.IO).launch {
                        dataProcessor.printResults(
                            selectedRaceViewModel.resultWrappers.value,
                            race
                        )
                    }
                }
            }

            R.id.result_menu_edit_race -> {
                findNavController().navigate(
                    BottomNavDirections.modifyRaceProperties(
                        RaceEditDialogFragment.RaceEditAcctions.EDIT,
                        0,
                        selectedRaceViewModel.race.value
                    )
                )
                return true
            }

            R.id.result_menu_global_settings -> {
                findNavController().navigate(BottomNavDirections.openSettingsFromRace())
                return true
            }

        }
        return false
    }

    private fun setResultListener() {
        //Enable event modification from menu
        setFragmentResultListener(RaceEditDialogFragment.REQUEST_RACE_MODIFICATION) { _, bundle ->
            val race: Race = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable(
                    RaceEditDialogFragment.BUNDLE_KEY_RACE,
                    Race::class.java
                )!!
            } else {
                bundle.getSerializable(RaceEditDialogFragment.BUNDLE_KEY_RACE) as Race
            }
            selectedRaceViewModel.updateRace(race)
        }
    }

    private fun setBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.race_end))
            val message = getString(R.string.race_end_confirmation)
            builder.setMessage(message)

            builder.setPositiveButton(R.string.general_ok) { dialog, _ ->
                selectedRaceViewModel.disableResultService()
                dataProcessor.removeCurrentRace()
                findNavController().navigate(ResultsFragmentDirections.closeRace())
            }

            builder.setNegativeButton(R.string.general_cancel) { dialog, _ ->
                dialog.cancel()
            }
            builder.show()
        }

    }

    private fun openReadoutDetail(competitorData: CompetitorData) {
        val resultData = ResultData(
            competitorData.readoutData!!.result,
            competitorData.readoutData!!.punches,
            competitorData.competitorCategory
        )
        findNavController().navigate(ResultsFragmentDirections.openReadoutDetail(resultData))
    }

    private fun setRecyclerViewAdapter() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                selectedRaceViewModel.resultWrappers.collect { results ->
                    resultsRecyclerView.adapter =
                        ResultsFragmentRecyclerViewAdapter(
                            ArrayList(results),
                            requireContext(),
                            selectedRaceViewModel
                        ) { cd -> openReadoutDetail(cd) }

                    (resultsRecyclerView.adapter as ResultsFragmentRecyclerViewAdapter).expandAllItems()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}