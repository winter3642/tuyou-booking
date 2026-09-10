# -*- coding: utf-8 -*-
"""
W4D3 压测用户准备：注册 N 个用户 → 登录拿 token → 可选为每个用户建一条购物车（超卖场景用）
输出 JMeter CSV Data Set 需要的 csv 文件。

用法：
    python scripts/bench/gen_users.py --count 200 --url http://localhost:8082 --out scripts/bench/users_qps.csv
    python scripts/bench/gen_users.py --count 1000 --url http://localhost:8082 --sku 1 --cart --out scripts/bench/users_oversell.csv
"""
import argparse
import concurrent.futures
import time

import requests


def one_user(i, base_url, sku, with_cart):
    """注册一个用户，返回 (token, cartId)。cartId 仅在 with_cart 时有值。"""
    # 用户名限 3-20 位：bench_ + 7位时间戳 + _ + 序号
    ts = int(time.time() * 1000) % 10 ** 7
    username = f"bench_{ts}_{i}"
    # 1. 注册
    r = requests.post(f"{base_url}/api/user/register",
                      json={"username": username, "password": "123456"}, timeout=30)
    r.raise_for_status()
    if r.json()["code"] != 0:
        raise RuntimeError(f"注册失败: {r.text[:100]}")
    # 2. 登录拿 token
    r = requests.post(f"{base_url}/api/user/login",
                      json={"username": username, "password": "123456"}, timeout=30)
    token = r.json()["data"]["token"]
    # 3. 可选：建购物车条目（下单接口吃 cartIds，超卖场景每用户预置一条）
    cart_id = None
    if with_cart:
        r = requests.post(f"{base_url}/api/cart",
                          headers={"Authorization": f"Bearer {token}"},
                          json={"skuId": sku, "quantity": 1}, timeout=30)
        if r.json()["code"] != 0:
            raise RuntimeError(f"加购失败: {r.text[:100]}")
        r = requests.get(f"{base_url}/api/cart",
                         headers={"Authorization": f"Bearer {token}"}, timeout=30)
        cart_id = r.json()["data"][0]["id"]
    return token, cart_id


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--count", type=int, required=True, help="用户数量")
    ap.add_argument("--url", default="http://localhost:8082")
    ap.add_argument("--sku", type=int, default=1, help="购物车 SKU id（--cart 时用）")
    ap.add_argument("--cart", action="store_true", help="为每个用户预置一条购物车")
    ap.add_argument("--out", required=True, help="输出 CSV 路径")
    args = ap.parse_args()

    t0 = time.time()
    header = "token" + (",cartId" if args.cart else "")
    done = 0
    with open(args.out, "w", encoding="utf-8") as f:
        f.write(header + "\n")
        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as pool:
            futures = [pool.submit(one_user, i, args.url, args.sku, args.cart)
                       for i in range(args.count)]
            for fut in concurrent.futures.as_completed(futures):
                token, cart_id = fut.result()
                f.write(token + (f",{cart_id}" if args.cart else "") + "\n")
                done += 1
                if done % 100 == 0:
                    print(f"[{time.time() - t0:6.1f}s] {done}/{args.count}", flush=True)
    print(f"完成 {done} 个用户 → {args.out}，耗时 {time.time() - t0:.1f}s")


if __name__ == "__main__":
    main()
