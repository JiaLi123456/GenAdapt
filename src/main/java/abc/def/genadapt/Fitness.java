package abc.def.genadapt;

public class Fitness extends ec.gp.koza.KozaFitness {

    public boolean isIdealFitness() {
        if (standardizedFitness < 2)
            return true;
        else
            return false;
    }
}
