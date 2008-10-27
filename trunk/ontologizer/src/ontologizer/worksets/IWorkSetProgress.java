package ontologizer.worksets;

public interface IWorkSetProgress
{
	void message(String message);
	void initGauge(int maxWork);
	void updateGauge(int currentWork);
}
