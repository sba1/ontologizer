package ontologizer.chisquare;

/**
 * This is a implementation of the Cumulative Distribution Function of
 * the chisquare distribution. Algorithm has been taken from "Numerical
 * recipes in C" (electonic version is freely available)
 *
 * @author Sebastian Bauer
 *
 */
public class ChiSquare2P
{
	static final double ITMAX = 100;
	static final double EPSILON = 3.0e-7;
	static final double FPMIN = 1.0e-30;

	static final double cof[] = new double[]
	{
		76.18009172947146, -86.50532032941677, 24.01409824083091,
		-1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5
	};

	/**
	 * Returns P(x>=chi).
	 *
	 * @param chi
	 * @param dof
	 * @return
	 */
	static public double pchi(double chi, int dof)
	{
		return gammq(dof / 2.0, chi / 2.0);
	}

	static private double gammln(double xx)
	{
		double x, y, temp, ser;

		int j;

		y = x = xx;
		temp = x + 5.5;
		temp -= (x + 0.5) * Math.log(temp);
		ser = 1.000000000190015;
		for (j = 0; j <= 5; j++)
			ser += cof[j] / ++y;
		return -temp + Math.log(2.5066282746310005 * ser / x);

	}

	static private double gammq(double a, double x)
	{
		double gln;

		if (x < 0.0 || a <= 0.0)
			throw new IllegalArgumentException("Invalid arguments in gammq");
		gln = gammln(a);

		if (x < (a + 1.0))
		{
			return 1 - gser(a, x, gln); // series representation
		} else
		{
			return gcf(a, x, gln); // continued fraction representation
		}
	}

	static private double gser(double a, double x, double gln)
	{
		int n;
		double sum, del, ap;

		if (x <= 0.0)
		{
			if (x < 0.0)
				throw new IllegalArgumentException("x less than 0 in routine gser");
			return 0.0;
		} else
		{
			ap = a;
			del = sum = 1.0 / a;
			for (n = 1; n <= ITMAX; n++)
			{
				++ap;
				del *= x / ap;
				sum += del;
				if (Math.abs(del) < Math.abs(sum) * EPSILON)
				{
					return sum * Math.exp(-x + a * Math.log(x) - (gln));
				}
			}
			throw new IllegalArgumentException("x less than 0 in routine gser");
		}
	}

	static private double gcf(double a, double x, double gln)
	{
		int i;
		double an, b, c, d, del, h;

		b = x + 1.0 - a;
		c = 1.0 / FPMIN;
		d = 1.0 / b;
		h = d;
		for (i = 1; i <= ITMAX; i++)
		{
			an = -i * (i - a);
			b += 2.0;
			d = an * d + b;
			if (Math.abs(d) < FPMIN)
				d = FPMIN;
			c = b + an / c;
			if (Math.abs(c) < FPMIN)
				c = FPMIN;
			d = 1.0 / d;
			del = d * c;
			h *= del;
			if (Math.abs(del - 1.0) < EPSILON)
				break;
		}
		if (i > ITMAX)
			throw new IllegalArgumentException("a too large, ITMAX too small in gcf");
		return Math.exp(-x + a * Math.log(x) - (gln)) * h;
	}
}
