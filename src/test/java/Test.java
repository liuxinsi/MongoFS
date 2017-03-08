package java;

import com.lxs.mongofs.DirUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 刘馨思
 */
public class Test {
    public static void main(String[] args) {

        Set<String> ss = new HashSet<>();
        ss.add("/");
        ss.add("/为命名");

        Set<String> filtedDirSet = ss
                .stream()
                .filter(s -> {
                    String dir = DirUtils.getDir(s);
                    return !dir.equals("/");
                }).collect(Collectors.toSet());
        System.out.println("::" + ss);
    }
}
