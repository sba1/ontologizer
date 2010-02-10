package ontologizer.gui.swt.result;

public interface ITableAction
{
	public void tableSave(String path);
	public void latexSave(String path);
	public void htmlSave(String path);
	public void tableAnnotatedSetSave(String path);
}
