import requests
import hashlib
import time

def generate_sign(params, app_secret):
    """生成MD5签名"""
    # 1. 对参数按key进行ASCII码排序
    sorted_params = sorted(params.items())
    # 2. 拼接字符串 'key1value1key2value2'
    sign_str = ''.join([f'{k}{v}' for k, v in sorted_params])
    # 3. 拼接 app_secret
    sign_str = app_secret + sign_str + app_secret
    # 4. MD5加密并转为大写
    return hashlib.md5(sign_str.encode('utf-8')).hexdigest().upper()

def get_jd_comments(sku_id, page=1, page_size=20):
    """获取京东商品评论（JOS API方案示例）"""
    app_key = "YOUR_APP_KEY"  # 替换为你的AppKey
    app_secret = "YOUR_APP_SECRET"  # 替换为你的AppSecret
    api_url = "https://api.jd.com/routerjson"  # JOS API调用地址

    params = {
        "method": "jingdong.ware.comment.get",
        "app_key": app_key,
        "access_token": "YOUR_ACCESS_TOKEN", # 根据应用类型获取
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "v": "2.0",
        "sign_method": "md5",
        "skuId": sku_id,
        "page": str(page),
        "pageSize": str(page_size),
        "sortType": "5",  # 5: 时间排序
    }
    params["sign"] = generate_sign(params, app_secret)

    try:
        response = requests.post(api_url, data=params)
        response.raise_for_status()
        data = response.json()
        # 根据实际返回结构解析评论列表
        comments = data.get("jingdong_ware_comment_get_response", {})\
                         .get("wareCommentResult", {})\
                         .get("comments", [])
        return comments
    except Exception as e:
        print(f"请求失败: {e}")
        return []

if __name__ == '__main__':
    # 测试一个商品ID
    comments = get_jd_comments("100012084348", page=1)
    for comment in comments:
        print(f"用户: {comment.get('nickname')}, 评分: {comment.get('score')}, 内容: {comment.get('content')[:50]}...")