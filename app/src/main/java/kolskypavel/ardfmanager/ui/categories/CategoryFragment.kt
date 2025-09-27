package kolskypavel.ardfmanager.ui.categories

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kolskypavel.ardfmanager.BottomNavDirections
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.databinding.FragmentCategoriesBinding
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel
import kolskypavel.ardfmanager.ui.races.RaceEditDialogFragment
import kotlinx.coroutines.launch

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val selectedRaceViewModel: SelectedRaceViewModel by activityViewModels()
    private val dataProcessor = DataProcessor.get()

    private lateinit var categoryToolbar: Toolbar
    private lateinit var categoryAddFab: FloatingActionButton
    private lateinit var categoryRecyclerView: RecyclerView

    private var mLastClickTime: Long = 0

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryToolbar = view.findViewById(R.id.category_toolbar)
        categoryAddFab = view.findViewById(R.id.category_btn_add)
        categoryRecyclerView = view.findViewById(R.id.category_recycler_view)

        categoryToolbar.inflateMenu(R.menu.fragment_menu_category)
        categoryToolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener setFragmentMenuActions(it)
        }

        categoryAddFab.setOnClickListener {
            //Prevent accidental double click
            if (SystemClock.elapsedRealtime() - mLastClickTime > 1000) {
                selectedRaceViewModel.getCurrentRace()?.let { race ->
                    findNavController().navigate(
                        CategoryFragmentDirections.modifyCategory(
                            true,
                            -1, null, "",
                            race
                        )
                    )
                }
            }
            mLastClickTime = SystemClock.elapsedRealtime()
        }

        selectedRaceViewModel.race.observe(viewLifecycleOwner) { race ->
            categoryToolbar.title = race?.name
            race?.let { categoryToolbar.subtitle = dataProcessor.raceTypeToString(it.raceType) }
        }
        setFragmentListener()
        setRecyclerViewAdapter()
        setBackButton()
    }

    private fun setFragmentMenuActions(menuItem: MenuItem): Boolean {

        when (menuItem.itemId) {
            R.id.category_menu_import_file -> {
                findNavController().navigate(CategoryFragmentDirections.importExportData())
                return true
            }

            R.id.category_menu_manage_aliases -> {
                selectedRaceViewModel.getCurrentRace()
                    ?.let { findNavController().navigate(CategoryFragmentDirections.manageAliases(it.id)) }
                return true
            }

            R.id.category_menu_create_standard_categories -> {
                findNavController().navigate(CategoryFragmentDirections.createStandardCategories())
                return true
            }

            R.id.category_menu_edit_race -> {
                findNavController().navigate(
                    BottomNavDirections.modifyRaceProperties(
                        false,
                        0,
                        selectedRaceViewModel.race.value
                    )
                )
                return true
            }


            R.id.category_menu_global_settings -> {
                findNavController().navigate(BottomNavDirections.openSettingsFromRace())
                return true
            }
        }
        return false
    }


    private fun setFragmentListener() {
        setFragmentResultListener(CategoryEditDialogFragment.REQUEST_CATEGORY_MODIFICATION) { _, bundle ->
            val create = bundle.getBoolean(CategoryEditDialogFragment.BUNDLE_KEY_CREATE)
            val position = bundle.getInt(CategoryEditDialogFragment.BUNDLE_KEY_POSITION)

            if (!create) {
                categoryRecyclerView.adapter?.notifyItemChanged(position)
            }
        }

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

    private fun setRecyclerViewAdapter() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                selectedRaceViewModel.categories.collect { categories ->
                    categoryRecyclerView.adapter =
                        CategoryRecyclerViewAdapter(
                            categories, { action, position, categoryData ->
                                recyclerViewContextMenuActions(
                                    action,
                                    position,
                                    categoryData
                                )
                            }, requireContext(),
                            selectedRaceViewModel
                        )
                }
            }
        }
    }

    private fun confirmCategoryDeletion(category: Category) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.category_delete))
        builder.setMessage(getString(R.string.category_delete_confirmation, category.name))

        builder.setPositiveButton(R.string.general_ok) { dialog, _ ->
            selectedRaceViewModel.deleteCategory(category.id, category.raceId)
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.general_cancel) { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun recyclerViewContextMenuActions(
        action: Int,
        position: Int,
        categoryData: CategoryData
    ) {
        when (action) {
            0 -> {
                selectedRaceViewModel.getCurrentRace()?.let { race ->
                    findNavController().navigate(
                        CategoryFragmentDirections.modifyCategory(
                            false,
                            position,
                            categoryData.category,
                            categoryData.category.controlPointsString,
                            race
                        )
                    )
                }
            }

            1 -> selectedRaceViewModel.duplicateCategory(categoryData)
            2 -> confirmCategoryDeletion(categoryData.category)
        }
    }

    private fun setBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.race_end))
            val message = getString(R.string.race_end_confirmation)
            builder.setMessage(message)

            builder.setPositiveButton(R.string.general_ok) { _, _ ->
                selectedRaceViewModel.disableResultService()
                dataProcessor.removeCurrentRace()
                findNavController().navigate(CategoryFragmentDirections.closeRace())
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