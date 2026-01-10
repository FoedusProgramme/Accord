package uk.akane.accord.setupwizard.fragments

import android.animation.LayoutTransition
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import uk.akane.accord.R
import uk.akane.accord.logic.hasMediaPermissionSeparation
import uk.akane.accord.logic.isAlbumPermissionGranted
import uk.akane.accord.logic.isEssentialPermissionGranted
import uk.akane.accord.ui.MainActivity
import uk.akane.cupertino.widget.utils.AnimationUtils

class PermissionPageFragment : Fragment() {

    private lateinit var storageConstraintLayout: ConstraintLayout
    private lateinit var musicConstraintLayout: ConstraintLayout
    private lateinit var albumConstraintLayout: ConstraintLayout

    private lateinit var storagePermissionButton: MaterialButton
    private lateinit var musicPermissionButton: MaterialButton
    private lateinit var albumPermissionButton: MaterialButton

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isSuccessful: Boolean ->
            handlePermissionSuccessful(isSuccessful)
        }

    private fun handlePermissionSuccessful(successful: Boolean) {
        if (successful) {
            if (requireContext().isEssentialPermissionGranted() && !musicPermissionButton.isChecked) {
                (requireActivity() as? MainActivity)?.updateLibrary()
                (parentFragment as? SetupWizardFragment)?.releaseContinueButton()
            }
            if (hasMediaPermissionSeparation() &&
                requireContext().isEssentialPermissionGranted() && !musicPermissionButton.isChecked) {
                AnimationUtils.createValAnimator(
                    if (musicPermissionButton.isChecked) allowedColor else allowColor,
                    if (musicPermissionButton.isChecked) allowColor else allowedColor,
                    isArgb = true
                ) { it1 ->
                    musicPermissionButton.backgroundTintList = ColorStateList.valueOf(
                        it1
                    )
                }
                AnimationUtils.createValAnimator(
                    if (musicPermissionButton.isChecked) onAllowedColor else onAllowColor,
                    if (musicPermissionButton.isChecked) onAllowColor else onAllowedColor,
                    isArgb = true,
                ) { it1 ->
                    musicPermissionButton.setTextColor(it1)
                }
                musicPermissionButton.isChecked = !musicPermissionButton.isChecked
                musicPermissionButton.text = if (musicPermissionButton.isChecked) allowedString else allowString
            } else if (
                !hasMediaPermissionSeparation() &&
                requireContext().isEssentialPermissionGranted() && !storagePermissionButton.isChecked) {
                AnimationUtils.createValAnimator(
                    if (storagePermissionButton.isChecked) allowedColor else allowColor,
                    if (storagePermissionButton.isChecked) allowColor else allowedColor,
                    isArgb = true,
                ) { it1 ->
                    storagePermissionButton.backgroundTintList = ColorStateList.valueOf(
                        it1
                    )
                }
                AnimationUtils.createValAnimator(
                    if (storagePermissionButton.isChecked) onAllowedColor else onAllowColor,
                    if (storagePermissionButton.isChecked) onAllowColor else onAllowedColor,
                    isArgb = true,
                ) { it1 ->
                    storagePermissionButton.setTextColor(it1)
                }
                storagePermissionButton.isChecked = !storagePermissionButton.isChecked
                storagePermissionButton.text = if (storagePermissionButton.isChecked) allowedString else allowString
            } else if (
                hasMediaPermissionSeparation() &&
                requireContext().isAlbumPermissionGranted() && !albumPermissionButton.isChecked) {
                AnimationUtils.createValAnimator(
                    if (albumPermissionButton.isChecked) allowedColor else allowColor,
                    if (albumPermissionButton.isChecked) allowColor else allowedColor,
                    isArgb = true
                ) { it1 ->
                    albumPermissionButton.backgroundTintList = ColorStateList.valueOf(
                        it1
                    )
                }
                AnimationUtils.createValAnimator(
                    if (albumPermissionButton.isChecked) onAllowedColor else onAllowColor,
                    if (albumPermissionButton.isChecked) onAllowColor else onAllowedColor,
                    isArgb = true,
                ) { it1 ->
                    albumPermissionButton.setTextColor(it1)
                }
                albumPermissionButton.isChecked = !albumPermissionButton.isChecked
                albumPermissionButton.text = if (albumPermissionButton.isChecked) allowedString else allowString
            }
        }
    }

    private var allowString = ""
    private var allowedString = ""

    private var allowColor = 0
    private var allowedColor = 0

    private var onAllowColor = 0
    private var onAllowedColor = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_setup_wizard_permission_page, container, false)

        allowString = getString(R.string.allow)
        allowedString = getString(R.string.allowed)

        allowColor = resources.getColor(R.color.setupWizardSurfaceColor, null)
        allowedColor = resources.getColor(R.color.accentColor, null)

        onAllowColor = resources.getColor(R.color.accentColor, null)
        onAllowedColor = resources.getColor(R.color.onAccentColor, null)

        storageConstraintLayout = rootView.findViewById(R.id.storage_card)
        musicConstraintLayout = rootView.findViewById(R.id.music_card)
        albumConstraintLayout = rootView.findViewById(R.id.photo_card)

        if (hasMediaPermissionSeparation()) {
            storageConstraintLayout.visibility = View.GONE
        } else {
            musicConstraintLayout.visibility = View.GONE
            albumConstraintLayout.visibility = View.GONE
        }

        storagePermissionButton = rootView.findViewById(R.id.storage_apply_btn)
        musicPermissionButton = rootView.findViewById(R.id.music_apply_btn)
        albumPermissionButton = rootView.findViewById(R.id.album_apply_btn)

        updateStatusImmediately()

        storageConstraintLayout.layoutTransition
            .enableTransitionType(LayoutTransition.CHANGING)
        musicConstraintLayout.layoutTransition
            .enableTransitionType(LayoutTransition.CHANGING)
        albumConstraintLayout.layoutTransition
            .enableTransitionType(LayoutTransition.CHANGING)

        musicPermissionButton.setOnClickListener {
            if (!requireContext().isEssentialPermissionGranted()) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_AUDIO)
            }
        }

        storagePermissionButton.setOnClickListener {
            if (!requireContext().isEssentialPermissionGranted()) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        albumPermissionButton.setOnClickListener {
            if (!requireContext().isAlbumPermissionGranted()) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()
        updateStatusImmediately()
    }

    private fun updateStatusImmediately() {
        if (hasMediaPermissionSeparation()) {
            storageConstraintLayout.visibility = View.GONE
            musicPermissionButton.isChecked = requireContext().isEssentialPermissionGranted()
            musicPermissionButton.text = if (musicPermissionButton.isChecked) allowedString else allowString
            albumPermissionButton.isChecked = requireContext().isAlbumPermissionGranted()
            albumPermissionButton.text = if (albumPermissionButton.isChecked) allowedString else allowString
        } else {
            musicConstraintLayout.visibility = View.GONE
            albumConstraintLayout.visibility = View.GONE
            storagePermissionButton.isChecked = requireContext().isEssentialPermissionGranted()
            storagePermissionButton.text = if (storagePermissionButton.isChecked) allowedString else allowString
        }
    }
}