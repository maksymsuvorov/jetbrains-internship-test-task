package fit.suvormak.renamecurrentcommitplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

class RenameCommitAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull() ?: return
        val root = repository.root

        val newMessage = Messages.showInputDialog(
            project,
            "Enter new commit message:",
            "Rename Current Commit",
            Messages.getQuestionIcon()
        ) ?: return

        val headSha = repository.currentRevision ?: return
        val tipSha = getBranchTipSha(project, root, repository)

        if (tipSha == null) {
            rewordOldCommit(project)
            return
        }

        val isHeadLatest = headSha == tipSha

        if (isHeadLatest) {
            amendCommit(project, root, newMessage)
        }
    }

    private fun getBranchTipSha(project: Project, root: VirtualFile, repository: GitRepository): String? {
        val branchName = repository.currentBranchName ?: return null
        val handler = GitLineHandler(project, root, GitCommand.REV_PARSE)

        handler.addParameters(branchName)

        val result = Git.getInstance().runCommand(handler)

        return if (result.success()) result.output.firstOrNull()?.trim() else null
    }

    private fun amendCommit(project: Project, root: VirtualFile, message: String) {
        val handler = GitLineHandler(project, root, GitCommand.COMMIT)
        handler.addParameters("--amend", "-m", message)
        val result = Git.getInstance().runCommand(handler)

        if (result.success()) {
            Messages.showInfoMessage(project, "Commit message amended successfully.", "Success")
        } else {
            Messages.showErrorDialog(project, "Failed to amend commit: ${result.errorOutputAsJoinedString}", "Error")
        }
    }

    private fun rewordOldCommit(project: Project) {
        Messages.showErrorDialog(
            project,
            "Rewording non-HEAD commits requires an interactive rebase, which is not implemented yet.",
            "Not Supported Yet"
        )
    }
}

