package kolskypavel.ardfmanager.ui.readouts

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kolskypavel.ardfmanager.BottomNavDirections
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kolskypavel.ardfmanager.databinding.FragmentReadoutsBinding
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel
import kolskypavel.ardfmanager.ui.races.RaceEditDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class ReadoutFragment : Fragment() {

    private var _binding: FragmentReadoutsBinding? = null
    private val selectedRaceViewModel: SelectedRaceViewModel by activityViewModels()
    private val dataProcessor = DataProcessor.get()

    private var statsJob: Job? = null

    private lateinit var readoutSwipeLayout: SwipeRefreshLayout
    private lateinit var readoutToolbar: Toolbar
    private lateinit var startedTextView: TextView
    private lateinit var limitTextView: TextView
    private lateinit var finishedTextView: TextView
    private lateinit var startedProgressBar: ProgressBar
    private lateinit var limitProgressBar: ProgressBar
    private lateinit var finishedProgressBar: ProgressBar
    private lateinit var readoutRecyclerView: RecyclerView
    private lateinit var readoutAddFab: FloatingActionButton


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val getResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val value = it.data
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentReadoutsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        readoutSwipeLayout = view.findViewById(R.id.readouts_swipe_layout)
        readoutToolbar = view.findViewById(R.id.readouts_toolbar)
        readoutRecyclerView = view.findViewById(R.id.readout_recycler_view)

        startedTextView = view.findViewById(R.id.readouts_started_text)
        finishedTextView = view.findViewById(R.id.readouts_finished_text)
        limitTextView = view.findViewById(R.id.readouts_limit_text)
        startedProgressBar = view.findViewById(R.id.readouts_started_progress_bar)
        finishedProgressBar = view.findViewById(R.id.readouts_finished_progress_bar)
        limitProgressBar = view.findViewById(R.id.readouts_limit_progress_bar)
        readoutAddFab = view.findViewById(R.id.readout_btn_add)

        readoutToolbar.inflateMenu(R.menu.fragment_menu_readout)
        readoutToolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener setFragmentMenuActions(it)
        }

        selectedRaceViewModel.race.observe(viewLifecycleOwner) { race ->
            readoutToolbar.title = race?.name
            race?.let { readoutToolbar.subtitle = dataProcessor.raceTypeToString(it.raceType) }
        }

        readoutSwipeLayout.setOnRefreshListener {
            selectedRaceViewModel.getCurrentRace()
                ?.let { race -> selectedRaceViewModel.updateResultsByRace(race.id) }
            readoutSwipeLayout.isRefreshing = false
        }

        readoutAddFab.setOnClickListener {
            selectedRaceViewModel.getCurrentRace()?.let { race ->
                findNavController().navigate(
                    ReadoutFragmentDirections.editOrCreateReadout(
                        true,
                        null,
                        -1,
                        race.id
                    )
                )
            }
        }

        setResultListener()
        setRecyclerAdapter()
        setBackButton()

        selectedRaceViewModel.getCurrentRace()?.let { race ->
            statsJob = getStatsJob(race.id)
            statsJob!!.start()
        }
    }

    private fun setFragmentMenuActions(menuItem: MenuItem): Boolean {

        when (menuItem.itemId) {

            R.id.readout_menu_delete_all_readouts -> {
                confirmAllReadoutDeletion()
                return true
            }

            R.id.readout_menu_edit_race -> {
                findNavController().navigate(
                    BottomNavDirections.modifyRaceProperties(
                        RaceEditDialogFragment.RaceEditAcctions.EDIT,
                        0,
                        selectedRaceViewModel.race.value
                    )
                )
                return true
            }

            R.id.readout_menu_global_settings -> {
                findNavController().navigate(BottomNavDirections.openSettingsFromRace())
                return true
            }

        }
        return false
    }

    private fun confirmAllReadoutDeletion() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.readout_delete_all))
        builder.setMessage(R.string.readout_delete_all_confirmation)

        builder.setPositiveButton(R.string.general_ok) { dialog, _ ->
            selectedRaceViewModel.deleteAllResultsByRace()
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.general_cancel) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun setResultListener() {
        //Enable race modification from menu
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

    private fun recyclerViewContextMenuActions(
        action: Int,
        position: Int,
        resultData: ResultData
    ) {
        when (action) {
            0 -> {
                findNavController().navigate(
                    ReadoutFragmentDirections.editOrCreateReadout(
                        false, resultData, position, resultData.result.raceId
                    )
                )
            }

            1 -> {
                confirmReadoutDeletion(resultData)
            }
        }
    }

    private fun getStatsJob(raceId: UUID): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                val statistics =
                    selectedRaceViewModel.getStatistics(raceId)

                startedTextView.text = "${statistics.startedCompetitors}/${statistics.competitors}"
                startedProgressBar.progress = if (statistics.startedCompetitors != 0) {
                    ((statistics.startedCompetitors / statistics.competitors.toDouble()) * 100).toInt()
                } else {
                    0
                }

                finishedTextView.text =
                    "${statistics.finishedCompetitors}/${statistics.competitors}"
                finishedProgressBar.progress = if (statistics.finishedCompetitors != 0) {
                    ((statistics.finishedCompetitors / statistics.competitors.toDouble()) * 100).toInt()
                } else {
                    0
                }

                limitTextView.text =
                    "${statistics.inLimitCompetitors}/${statistics.startedCompetitors - statistics.finishedCompetitors}"
                limitProgressBar.progress =
                    if (statistics.startedCompetitors - statistics.finishedCompetitors != 0) {
                        ((statistics.inLimitCompetitors / (statistics.startedCompetitors - statistics.finishedCompetitors).toDouble()) * 100).toInt()
                    } else {
                        100
                    }
                delay(3000)
            }
        }
    }

    private fun confirmReadoutDeletion(resultData: ResultData) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.readout_delete_readout))
        val message =
            getString(
                R.string.readout_delete_readout_confirmation,
                resultData.result.siNumber
            )
        builder.setMessage(message)

        builder.setPositiveButton(R.string.general_ok) { dialog, _ ->
            selectedRaceViewModel.deleteResult(resultData.result.id)
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.general_cancel) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun setRecyclerAdapter() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                selectedRaceViewModel.readoutData.collect { readouts ->
                    readoutRecyclerView.adapter =
                        ReadoutDataRecyclerViewAdapter(
                            readouts,
                            requireContext(),
                            { readoutData -> openReadoutDetail(readoutData) },
                            { action, position, readoutData ->
                                recyclerViewContextMenuActions(
                                    action,
                                    position,
                                    readoutData
                                )
                            })
                }
            }
        }
    }

    private fun openReadoutDetail(resultData: ResultData) {
        findNavController().navigate(ReadoutFragmentDirections.openReadoutDetail(resultData))
    }

    private fun setBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.race_end))
            val message = getString(R.string.race_end_confirmation)
            builder.setMessage(message)

            builder.setPositiveButton(R.string.general_ok) { dialog, _ ->
                selectedRaceViewModel.disableResultService()
                selectedRaceViewModel.removeReaderRace()
                statsJob?.cancel()

                findNavController().navigate(ReadoutFragmentDirections.closeRace())
            }

            builder.setNegativeButton(R.string.general_cancel) { dialog, _ ->
                dialog.cancel()
            }
            builder.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}