package withoutc.chongchong.support.openapi;

record ApiOperation(String method, String path) implements Comparable<ApiOperation> {

    @Override
    public int compareTo(ApiOperation other) {
        int methodComparison = method.compareTo(other.method);
        if (methodComparison != 0) {
            return methodComparison;
        }
        return path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return method + " " + path;
    }
}
