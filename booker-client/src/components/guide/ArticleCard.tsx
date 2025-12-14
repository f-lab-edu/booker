interface Author {
  name: string;
  role: string;
  avatar?: string;
}

interface ArticleCardProps {
  title: string;
  subtitle: string;
  author: Author;
  date: string;
  category: string;
  content: string;
  bannerGradient?: string;
  bannerIcon?: string;
}

export function ArticleCard({
  title,
  subtitle,
  author,
  date,
  category,
  content,
  bannerGradient = 'from-pink-300 via-purple-200 to-orange-200',
  bannerIcon,
}: ArticleCardProps) {
  return (
    <article className="max-w-4xl">
      {/* Banner Image */}
      <div className={`relative w-full h-96 rounded-2xl bg-gradient-to-br ${bannerGradient} mb-8 flex items-center justify-center overflow-hidden`}>
        {bannerIcon && (
          <div className="bg-white/90 rounded-3xl p-12">
            <div className="text-8xl">{bannerIcon}</div>
          </div>
        )}
        {/* Share Button */}
        <button className="absolute top-6 right-6 px-4 py-2 bg-white/80 backdrop-blur-sm rounded-lg text-gray-700 hover:bg-white transition-colors flex items-center gap-2">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
          </svg>
          <span className="text-sm font-medium">공유하기</span>
        </button>
      </div>

      {/* Title */}
      <h1 className="text-5xl font-bold text-gray-900 mb-4 leading-tight">
        {title}
      </h1>

      {/* Subtitle */}
      <p className="text-xl text-gray-600 mb-8">
        {subtitle}
      </p>

      {/* Author Info */}
      <div className="flex items-center gap-4 mb-6">
        <div className="w-12 h-12 rounded-full bg-gray-200 overflow-hidden">
          {author.avatar ? (
            <img src={author.avatar} alt={author.name} className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-500 text-lg font-semibold">
              {author.name.charAt(0)}
            </div>
          )}
        </div>
        <div>
          <div className="text-sm font-medium text-gray-900">
            {author.name} • {author.role}
          </div>
          <div className="text-sm text-gray-500">{date}</div>
        </div>
      </div>

      {/* Category Tag */}
      <div className="mb-8">
        <span className="inline-block px-4 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm">
          {category}
        </span>
      </div>

      {/* Content */}
      <div className="prose prose-lg max-w-none text-gray-700 leading-relaxed">
        {content.split('\n').map((paragraph, index) => (
          paragraph.trim() && (
            <p key={index} className="mb-6">
              {paragraph}
            </p>
          )
        ))}
      </div>
    </article>
  );
}
