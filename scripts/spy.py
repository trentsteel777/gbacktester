import pandas as pd
import requests

def list_slickcharts_sp500() -> pd.DataFrame:
    url = 'https://www.slickcharts.com/sp500'
    user_agent = 'Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/111.0'
    response = requests.get(url, headers={'User-Agent': user_agent})
    return pd.read_html(response.text, match='Symbol', index_col='Symbol')[0]

df = list_slickcharts_sp500()

# Convert to a Java-style list
symbols = sorted(df.index.tolist())

print(",".join(symbols))
