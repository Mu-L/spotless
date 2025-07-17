package com.diffplug.spotless;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Abstract class responsible for installing a Git pre-push hook in a repository.
 * This class ensures that specific checks and logic are run before a push operation in Git.
 *
 * Subclasses should define specific behavior for hook installation by implementing the required abstract methods.
 */
public abstract class GitPrePushHookInstaller {

	/**
	 * Logger for recording informational and error messages during the installation process.
	 */
	protected final GitPreHookLogger logger;

	/**
	 * The root directory of the Git repository where the hook will be installed.
	 */
	protected final File root;

	/**
	 * Constructor to initialize the GitPrePushHookInstaller with a logger and repository root path.
	 *
	 * @param logger The logger for recording messages.
	 * @param root   The root directory of the Git repository.
	 */
	public GitPrePushHookInstaller(GitPreHookLogger logger, File root) {
		this.logger = logger;
		this.root = root;
	}

	/**
	 * Installs the Git pre-push hook into the repository.
	 *
	 * <p>This method checks for the following:
	 * <ul>
	 *   <li>Ensures Git is installed and the `.git/config` file exists.</li>
	 *   <li>Checks if an executor required by the hook is available.</li>
	 *   <li>Creates and writes the pre-push hook file if it does not exist.</li>
	 *   <li>Skips installation if the hook is already installed.</li>
	 * </ul>
	 * If an issue occurs during installation, error messages are logged.
	 *
	 * @throws Exception if any error occurs during installation.
	 */
	public void install() throws Exception {
		logger.info("Installing git pre-push hook");

		if (!isGitInstalled()) {
			logger.error("Git not found in root directory");
			return;
		}

		if (!isExecutorInstalled()) {
			return;
		}

		var hookContent = "";
		final var gitHookFile = root.toPath().resolve(".git/hooks/pre-push").toFile();
		if (!gitHookFile.exists()) {
			logger.info("Git pre-push hook not found, creating it");
			gitHookFile.getParentFile().mkdirs();
			if (!gitHookFile.createNewFile()) {
				logger.error("Failed to create pre-push hook file");
				return;
			}

			if (!gitHookFile.setExecutable(true, false)) {
				logger.error("Can not make file executable");
				return;
			}

			hookContent += "#!/bin/sh\n";
		}

		if (isGitHookInstalled(gitHookFile)) {
			logger.info("Skipping, git pre-push hook already installed %s", gitHookFile.getAbsolutePath());
			return;
		}

		hookContent += preHookContent();
		writeFile(gitHookFile, hookContent);

		logger.info("Git pre-push hook installed successfully to the file %s", gitHookFile.getAbsolutePath());
	}

	/**
	 * Checks if the required executor for performing the desired pre-push actions is installed.
	 *
	 * @return {@code true} if the executor is installed, {@code false} otherwise.
	 */
	protected abstract boolean isExecutorInstalled();

	/**
	 * Provides the content of the hook that should be inserted into the pre-push script.
	 *
	 * @return A string representing the content to include in the pre-push script.
	 */
	protected abstract String preHookContent();

	/**
	 * Checks if Git is installed by validating the existence of `.git/config` in the repository root.
	 *
	 * @return {@code true} if Git is installed, {@code false} otherwise.
	 */
	private boolean isGitInstalled() {
		return root.toPath().resolve(".git/config").toFile().exists();
	}

	/**
	 * Verifies if the pre-push hook file already contains the custom Spotless hook content.
	 *
	 * @param gitHookFile The file representing the Git hook.
	 * @return {@code true} if the hook is already installed, {@code false} otherwise.
	 * @throws Exception if an error occurs when reading the file.
	 */
	private boolean isGitHookInstalled(File gitHookFile) throws Exception {
		final var hook = Files.readString(gitHookFile.toPath(), UTF_8);
		return hook.contains("##### SPOTLESS HOOK START #####");
	}

	/**
	 * Writes the specified content into a file.
	 *
	 * @param file    The file to which the content should be written.
	 * @param content The content to write into the file.
	 * @throws IOException if an error occurs while writing to the file.
	 */
	private void writeFile(File file, String content) throws IOException {
		try (final var writer = new FileWriter(file, UTF_8, true)) {
			writer.write(content);
		}
	}

	/**
	 * Generates a pre-push template script that defines the commands to check and apply changes
	 * using an executor and Spotless.
	 *
	 * @param executor      The tool to execute the check and apply commands.
	 * @param commandCheck  The command to check for issues.
	 * @param commandApply  The command to apply corrections.
	 * @return A string template representing the Spotless Git pre-push hook content.
	 */
	protected String preHookTemplate(String executor, String commandCheck, String commandApply) {
		var spotlessHook = "\n";
		spotlessHook += "\n##### SPOTLESS HOOK START #####";
		spotlessHook += "\nSPOTLESS_EXECUTOR=" + executor;
		spotlessHook += "\nif ! $SPOTLESS_EXECUTOR " + commandCheck + " ; then";
		spotlessHook += "\n    echo 1>&2 \"spotless found problems, running " + commandApply + "; commit the result and re-push\"";
		spotlessHook += "\n    $SPOTLESS_EXECUTOR " + commandApply;
		spotlessHook += "\n    exit 1";
		spotlessHook += "\nfi";
		spotlessHook += "\n##### SPOTLESS HOOK END #####";
		spotlessHook += "\n\n";
		return spotlessHook;
	}

	public interface GitPreHookLogger {
		void info(String format, Object... arguments);
		void error(String format, Object... arguments);
	}
}
