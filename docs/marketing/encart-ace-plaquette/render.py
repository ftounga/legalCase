import asyncio, pathlib
from playwright.async_api import async_playwright

base = pathlib.Path("/home/francky/dev/legalCase/docs/marketing/encart-ace-plaquette")
html_path = (base / "encart-ace.html").as_uri()

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch()
        # PNG haute résolution (x2)
        page = await b.new_page(device_scale_factor=2, viewport={"width":1080,"height":720})
        await page.goto(html_path)
        await page.wait_for_timeout(1200)  # laisse charger les Google Fonts
        card = await page.query_selector(".card")
        await card.screenshot(path=str(base / "encart-ace-legalcase.png"))
        # PDF print-ready
        await page.pdf(path=str(base / "encart-ace-legalcase.pdf"),
                       print_background=True, width="1080px", height="760px", margin={"top":"0","bottom":"0","left":"0","right":"0"})
        await b.close()
    print("PNG + PDF rendus")

asyncio.run(main())
