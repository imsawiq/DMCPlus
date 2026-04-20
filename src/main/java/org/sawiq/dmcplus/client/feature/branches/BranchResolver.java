package org.sawiq.dmcplus.client.feature.branches;

public final class BranchResolver {

    private BranchResolver() {
    }

    public static BranchContext resolve(double x, double z) {
        Branch branch;
        if (z <= 0.0D && Math.abs(z) >= Math.abs(x)) {
            branch = Branch.YELLOW;
        } else if (x >= 0.0D && x >= Math.abs(z)) {
            branch = Branch.RED;
        } else if (z >= 0.0D && Math.abs(z) >= Math.abs(x)) {
            branch = Branch.GREEN;
        } else {
            branch = Branch.BLUE;
        }

        return new BranchContext(branch, branch.axisValue(x, z), branch.normalizedDistance(x, z));
    }
}
