package sonarquberepair;

import java.lang.reflect.Constructor;
import java.io.File;
import java.util.List;

import spoon.Launcher;
import spoon.processing.Processor;
import spoon.support.sniper.SniperJavaPrettyPrinter;
import spoon.support.JavaOutputProcessor;
import spoon.support.QueueProcessingManager;
import spoon.processing.ProcessingManager;
import spoon.reflect.factory.Factory;
import spoon.reflect.CtModel;

public class DefaultRepair {
	private final GitPatchGenerator generator = new GitPatchGenerator();
	private SonarQubeRepairConfig config;
	private int patchCounter = 1;

	public DefaultRepair(SonarQubeRepairConfig config) {
		this.config = config;
	}

	public void repair() throws Exception {
		File outputDir = new File(this.config.getWorkspace() + File.separator + "spooned");

		Launcher launcher = new Launcher();
		
		launcher.addInputResource(this.config.getOriginalFilesPath());
		launcher.setSourceOutputDirectory(outputDir.getAbsolutePath());
		launcher.getEnvironment().setAutoImports(true);
		if (this.config.getPrettyPrintingStrategy() == PrettyPrintingStrategy.SNIPER) {
			launcher.getEnvironment().setPrettyPrinterCreator(() -> {
				SniperJavaPrettyPrinter sniper = new SniperJavaPrettyPrinter(launcher.getEnvironment());
				sniper.setIgnoreImplicit(false);
				return sniper;
			}
			);
			launcher.getEnvironment().setCommentEnabled(true);
			launcher.getEnvironment().useTabulations(true);
			launcher.getEnvironment().setTabulationSize(4);
		}

		Class<?> processor = Processors.getProcessor(config.getRuleKeys().get(0));
		Constructor<?> cons;
		Object object;
		try {
			cons = processor.getConstructor(String.class);
			object = cons.newInstance(config.getProjectKey());
		} catch (NoSuchMethodException e) {
			cons = processor.getConstructor();
			object = cons.newInstance();
		}

		CtModel model = launcher.buildModel();
		Factory factory = launcher.getFactory();
		ProcessingManager processingManager = new QueueProcessingManager(factory);
		JavaOutputProcessor javaOutputProcessor = launcher.createOutputWriter();
		processingManager.addProcessor((Processor) object);
		processingManager.process(factory.Class().getAll());

		if (this.config.getFileOutputStrategy() == FileOutputStrategy.CHANGED_ONLY) {
			for (String inputPath : UniqueTypesCollector.getInstance().getTopLevelTypes4Output().keySet()) {
				javaOutputProcessor.process(UniqueTypesCollector.getInstance().getTopLevelTypes4Output().get(inputPath));

				/* if also generating git patches */
				File patchDir = new File(this.config.getWorkspace() + File.separator + "SonarGitPatches");

				if (!patchDir.exists()) {
					patchDir.mkdirs();
				}
				List<File> list = javaOutputProcessor.getCreatedFiles();
				if (!list.isEmpty()) {
					String outputPath = list.get(list.size() - 1).getAbsolutePath();
					if (this.config.getGitRepoPath() != null) {
						this.generator.setGitProjectRootDir(this.config.getGitRepoPath());
						generator.generate(inputPath,outputPath, patchDir.getAbsolutePath() + File.separator + "sonarpatch_" + this.patchCounter);
						this.patchCounter++;
					}
				}
			}
		} else {
			processingManager.addProcessor(javaOutputProcessor);
			processingManager.process(factory.Class().getAll());
		}


		UniqueTypesCollector.getInstance().reset();
	}
}