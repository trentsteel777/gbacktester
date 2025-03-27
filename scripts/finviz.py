import requests
import re
import time

def fetch_html(url):
    headers = {'User-Agent': 'Mozilla/5.0'}
    try:
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        return response.text
    except requests.RequestException as e:
        print(f"Error fetching URL: {e}")
        return ""

def extract_tickers(html_content):
    tickers = []
    # Updated regular expression to extract the content between <!-- TS and TE -->
    ts_regex = re.compile(r'<!--\s*TS([\s\S]*?)TE\s*-->')
    match = ts_regex.search(html_content)
    if match:
        ts_content = match.group(1)
        # Split the content into lines
        for line in ts_content.splitlines():
            # Find the ticker symbol (everything before the first '|')
            if '|' in line:
                ticker = line.split('|')[0].strip()
                tickers.append(ticker)
    else:
        print("TS section not found in the HTML content.")
    return tickers

def is_last_page(html_content):
    # Regular expression to match the 'selected' option with the page number
    page_regex = re.compile(r'<option selected="selected" value="\d+">Page \d+ / \d+</option>')
    return bool(page_regex.search(html_content))

def is_rate_limited(html_content):
    # Regex to match the phrase "rate limited"
    rate_limit_regex = re.compile(r"rate limited", re.IGNORECASE)
    return bool(rate_limit_regex.search(html_content))

def main():
    r_value = 1  # Starting value for r
    filename = "universe.txt"

    # Open universe.txt file in append mode
    with open(filename, "a") as universe_file:
        while True:
            url = f"https://finviz.com/screener.ashx?v=111&r={r_value}"
            html_content = fetch_html(url)

            if is_rate_limited(html_content):
                print("Rate limit reached. Exiting program.")
                break

            tickers = extract_tickers(html_content)
            print(f"Extracted Tickers ({r_value}):")
            for ticker in tickers:
                print(ticker)
                universe_file.write(ticker + "\n")

            if is_last_page(html_content) or len(tickers) == 1:
                print("Last page found, exiting.")
                break

            r_value += 20  # Increment r_value by 20 for the next iteration
            time.sleep(1.5)  # Sleep for 1.5 seconds before the next request

if __name__ == "__main__":
    main()
