package echowand.util;

import java.util.Collection;
import java.util.LinkedList;

/**
 * コレクションから指定された要素のみを抽出したリストを作成
 * @param <T> 要素の型
 * @author Yoshiki Makino
 */
public class Collector<T> {
    private Selector<T> selector;
    
    /**
     * 指定されたSelectorを利用するCollectorを生成する。
     * @param selector Selectorの指定
     */
    public Collector(Selector<T> selector) {
        this.selector = selector;
    }
    
    /**
     * 指定されたオブジェクトの集合から、選択されたオブジェクトのリストを作成し返す。
     * コンストラクタで指定されたSelectorを利用してオブジェクトの選択を行う。
     * @param objects 指定されたオブジェクト集合
     * @return 選択されたオブジェクトのリスト
     */
    public LinkedList<T> collect(Collection<T> objects) {
        LinkedList<T> newList = new LinkedList<T>();
        for (T object : objects) {
            if (selector.select(object)) {
                newList.add(object);
            }
        }
        return newList;
    }
}
