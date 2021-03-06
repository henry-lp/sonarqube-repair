package sonarquberepair.processor.sonarbased;

import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;

public class DeadStoreProcessor extends SonarWebAPIBasedProcessor<CtStatement> {

	public DeadStoreProcessor(String projectKey) {
		super(1854, projectKey);
	}

	@Override
	public boolean isToBeProcessed(CtStatement element) {
		if (element == null) {
			return false;
		}
		if (!(element instanceof CtLocalVariable) && !(element instanceof CtAssignment)) {
			return false;
		}
		return super.isToBeProcessedAccordingToSonar(element);
	}

	@Override
	public void process(CtStatement element) {
		super.process(element);
		element.delete();
	}

}
