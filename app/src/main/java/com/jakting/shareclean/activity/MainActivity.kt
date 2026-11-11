package com.jakting.shareclean.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakting.shareclean.BaseActivity
import com.jakting.shareclean.BuildConfig
import com.jakting.shareclean.R
import com.jakting.shareclean.databinding.ActivityMainBinding
import com.jakting.shareclean.utils.*
import com.jakting.shareclean.utils.application.Companion.kv
import com.jakting.shareclean.utils.application.Companion.shell
import com.mikepenz.aboutlibraries.LibsBuilder
import kotlinx.coroutines.launch


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        lifecycleScope.launch {
            getAppIcon(BuildConfig.APPLICATION_ID)?.let {
                binding.appSelfIcon.setImageDrawable(it)
            }
        }

        checkStatus()
        binding.contentMain.card2ManageClean.cardManager.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            startActivity(Intent(this, CleanManagerActivity::class.java))
        }
        binding.contentMain.card3ManageIntent.cardManager.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            startActivity(Intent(this, QuickCleanActivity::class.java))
        }

        binding.contentMain.card4List.cardList1.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.misc_backup_and_restore_title)
                .setMessage(R.string.misc_backup_and_restore_msg)
                .setPositiveButton(R.string.misc_backup_and_restore_backup) { _, _ ->
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        val time = System.currentTimeMillis() //用于 备份&恢复 的时间戳
                        putExtra(Intent.EXTRA_TITLE, "TigerInTheWall_backup_$time.json")
                    }
                    backupResultLauncher.launch(intent)
                }
                .setNegativeButton(R.string.misc_backup_and_restore_restore) { _, _ ->
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    restoreResultLauncher.launch(intent)
                }
                .show()
        }
        binding.contentMain.card4List.cardList2.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.contentMain.card4List.cardList3.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            MaterialAlertDialogBuilder(this)
                .setMessage(
                    String.format(
                        getString(R.string.about_message),
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                    )
                )
                .setPositiveButton(R.string.ok) { _, _ -> }
                .setNeutralButton(R.string.about_lib) { _, _ ->
                    LibsBuilder()
                        .start(this)
                }
                .show()

        }
    }

    private var backupResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intent = result.data
            if (intent != null && result.resultCode == Activity.RESULT_OK) {
                toast(getString(R.string.please_wait))
                if (backupTIW(intent.data as Uri)) {
                    toast(getString(R.string.misc_backup_and_restore_backup_ok))
                } else {
                    toast(getString(R.string.misc_backup_and_restore_error))
                }
            }
        }

    private var restoreResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intent = result.data
            if (intent != null && result.resultCode == Activity.RESULT_OK) {
                toast(getString(R.string.please_wait))
                if (restoreTIW(intent.data as Uri)) {
                    if (deleteIfwFiles("all") && writeIfwFiles()) {
                        toast(getString(R.string.misc_backup_and_restore_restore_ok))
                    }
                } else {
                    toast(getString(R.string.misc_backup_and_restore_error))
                }
            }
        }

    @SuppressLint("SetTextI18n")
    private fun checkStatus() {
        if (shell.isRoot) {
            // 已经授予 root
            if (moduleApplyAvailable()) {
                // 如果模块已生效
                binding.contentMain.card1Module.cardStatusTitle.text =
                    getString(R.string.status_card_exist)
                val injectIf = moduleInfo()
                // 尝试请求 Riru 目录，如果 Riru 可用，则说明 IFW Enchance 是 Riru 版本
                if (injectIf[0].isNotEmpty()) {
                    binding.contentMain.card1Module.cardStatusDesc.text =
                        String.format(
                            getString(R.string.status_card_exist_module),
                            injectIf[1],
                            injectIf[2]
                        )
                    binding.contentMain.card1Module.cardStatusInjectWhich.text = injectIf[0]
                    binding.contentMain.card1Module.cardStatusIcon.setImageResource(R.drawable.ic_twotone_check_circle_24)
                    binding.contentMain.card1Module.cardStatus.backgroundTintList =
                        ColorStateList.valueOf(getColorFromAttr(R.attr.colorPrimary))
                    var clickCount = 0
                    binding.contentMain.card1Module.cardStatus.setOnClickListener {
                        clickCount++
                        when (clickCount) {
                            5 -> {
                                binding.contentMain.card1Module.cardStatusInjectWhich.text =
                                    injectIf[0] + "🤥"
                            }

                            10 -> {
                                binding.contentMain.card1Module.cardStatusInjectWhich.text =
                                    injectIf[0] + "🤕"
                            }

                            15 -> {
                                binding.contentMain.card1Module.cardStatusInjectWhich.text =
                                    injectIf[0] + "🤡"
                            }

                            20 -> {
                                binding.contentMain.card1Module.cardStatusInjectWhich.text =
                                    injectIf[0] + "👻"
                                toast(getString(R.string.status_card_click))
                                clickCount = 0
                            }
                        }
                        it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                }
            } else {
                binding.contentMain.card1Module.cardStatusTitle.text =
                    getString(R.string.status_card_no_module)
                binding.contentMain.card1Module.cardStatus.setOnClickListener {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.status_card_dialog_no_module_title))
                        .setMessage(getString(R.string.status_card_dialog_no_module_content))
                        .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.status_card_dialog_more)) { _, _ ->
                            openLink(getString(R.string.status_card_dialog_more_url))
                        }
                        .show()
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }


        } else {
            //没有授予 root 的时候，点击卡片会弹窗
            binding.contentMain.card1Module.cardStatusTitle.text =
                getString(R.string.status_card_no_root)
            binding.contentMain.card1Module.cardStatusDesc.text =
                getString(R.string.status_card_no_root_detail)
            binding.contentMain.card1Module.cardStatusInject.visibility = View.GONE
            binding.contentMain.card1Module.cardStatusInjectWhich.visibility = View.GONE
            if (!kv.decodeBool("root_hint_shown", false)) {
                kv.encode("root_hint_shown", true)
                toast(getString(R.string.status_card_no_root_toast))
            }
            binding.contentMain.card1Module.cardStatus.setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.status_card_dialog_title))
                    .setMessage(getString(R.string.status_card_dialog_content))
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.status_card_dialog_more)) { _, _ ->
                        openLink(getString(R.string.status_card_dialog_more_url))
                    }
                    .show()
            }
        }
    }
}
