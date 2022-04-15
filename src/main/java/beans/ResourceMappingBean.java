
/**
 * リソースマッピング用 Beansクラス
 */
public class ResourceMappingBean {
    /** uuid */
    public String       uuid;

    /** 素材ファイル名 */
    public ResourceFilenamesBean fileNamesBean;

    /** 優先度 */
    public Integer      priority;

    /** 戻りデータ用のファイル名リスト */
    public String[]     previouts;

    /** 前のビーコンの案内画像ファイル名 **/
    public String       previousGuildImage;

    /** 次データ用のファイル名リスト */
    public String[]     next;

    /** 次のビーコンの案内画像ファイル名 **/
    public String       nextGuildImage;

    /** 予期せぬエラー時用の素材ファイル名 */
    public String       error;
}
